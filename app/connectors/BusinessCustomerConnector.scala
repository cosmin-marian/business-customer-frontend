package connectors

import audit.Auditable
import config.{BusinessCustomerFrontendAuditConnector, WSHttp}
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import utils.GovernmentGatewayConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessCustomerConnector extends BusinessCustomerConnector {
  val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  val appName: String = AppName.appName
  val serviceUrl = baseUrl("business-customer")
  val baseUri = "business-customer"
  val registerUri = "register"
  val knownFactsUri = "known-facts"
  val updateRegistrationDetailsURI = "update"
  val http: HttpGet with HttpPost = WSHttp
}

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def baseUri: String

  def registerUri: String

  def updateRegistrationDetailsURI: String

  def knownFactsUri: String

  def http: HttpGet with HttpPost

  def addKnownFacts(knownFacts: KnownFactsForService)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/${GovernmentGatewayConstants.KnownFactsAgentServiceName}/$knownFactsUri"""
    val jsonData = Json.toJson(knownFacts)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      auditAddKnownFactsCall(knownFacts, response)
      response
    }
  }


  def register(registerData: BusinessRegistrationRequest, service: String, isNonUKClientRegisteredByAgent: Boolean = false)
              (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[BusinessRegistrationResponse] = {

    def auditRegisterCall(input: BusinessRegistrationRequest,
                          response: HttpResponse,
                          service: String,
                          isNonUKClientRegisteredByAgent: Boolean = false)(implicit hc: HeaderCarrier) = {
      val eventType = response.status match {
        case OK => EventTypes.Succeeded
        case _ => EventTypes.Failed
      }
      val transactionName = input.address.countryCode.toUpperCase match {
        case "GB" => "etmpRegisterUKCall"
        case _ => if (isNonUKClientRegisteredByAgent) "etmpClientRegisteredByAgent" else "etmpRegisterNonUKCall"
      }

      sendDataEvent(transactionName = transactionName,
        detail = Map("txName" -> transactionName,
          "service" -> s"$service",
          "address" -> s"${input.address}",
          "contactDetails" -> s"${input.contactDetails}",
          "identification" -> s"${input.identification}",
          "isAGroup" -> s"${input.isAGroup}",
          "isAnAgent" -> s"${input.isAnAgent}",
          "organisation" -> s"${input.organisation}",
          "responseStatus" -> s"${response.status}",
          "responseBody" -> s"${response.body}",
          "status" ->  s"${eventType}"))

      def getAddressPiece(piece: Option[String]):String = {
        if (piece.isDefined)
          piece.get
        else
          ""
      }

      sendDataEvent(transactionName = if (input.address.postalCode.isDefined) "manualAddressSubmitted" else "internationalAddressSubmitted",
        detail = Map(
          "submittedLine1" -> input.address.addressLine1.toString,
          "submittedLine2" -> input.address.addressLine2.toString,
          "submittedLine3" -> getAddressPiece(input.address.addressLine3),
          "submittedLine4" -> getAddressPiece(input.address.addressLine4),
          "submittedPostcode" -> getAddressPiece(input.address.postalCode),
          "submittedCountry" -> input.address.countryCode))
    }

    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$registerUri"""
    val jsonData = Json.toJson(registerData)
    http.POST(postUrl, jsonData) map { response =>
      auditRegisterCall(registerData, response, service, isNonUKClientRegisteredByAgent)
      response.status match {
        case OK => response.json.as[BusinessRegistrationResponse]
        case NOT_FOUND =>
          Logger.warn(s"[BusinessCustomerConnector][register] - Not Found Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"${Messages("bc.connector.error.not-found")}  Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          Logger.warn(s"[BusinessCustomerConnector][register] - Service Unavailable Exception ${registerData.organisation.organisationName}")
          throw new ServiceUnavailableException(s"${Messages("bc.connector.error.service-unavailable")}  Exception ${response.body}")
        case BAD_REQUEST | INTERNAL_SERVER_ERROR =>
          Logger.warn(s"[BusinessCustomerConnector][register] - Bad Request Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"${Messages("bc.connector.error.bad-request")}  Exception ${response.body}")
        case status =>
          Logger.warn(s"[BusinessCustomerConnector][register] - $status Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)}  Exception ${response.body}")
      }
    }
  }

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[UpdateRegistrationResponse] = {
    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$updateRegistrationDetailsURI/$safeId"""
    val jsonData = Json.toJson(updateRegistrationDetails)
    http.POST(postUrl, jsonData) map { response =>
      response.status match {
        case OK => response.json.as[UpdateRegistrationResponse]
        case NOT_FOUND =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - Not Found Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new InternalServerException(s"${Messages("bc.connector.error.not-found")}  Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - Service Unavailable Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new ServiceUnavailableException(s"${Messages("bc.connector.error.service-unavailable")}  Exception ${response.body}")
        case BAD_REQUEST | INTERNAL_SERVER_ERROR =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - Bad Request Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new InternalServerException(s"${Messages("bc.connector.error.bad-request")}  Exception ${response.body}")
        case status =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - $status Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)}  Exception ${response.body}")
      }
    }
  }


  private def auditAddKnownFactsCall(input: KnownFactsForService, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "ggAddKnownFactsCall",
      detail = Map("txName" -> "ggAddKnownFactsCall",
        "facts" -> s"${input.facts}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" ->  s"${eventType}"))
  }



}
