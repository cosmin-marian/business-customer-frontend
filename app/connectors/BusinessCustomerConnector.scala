package connectors

import audit.Auditable
import config.{BusinessCustomerFrontendAuditConnector, WSHttp}
import models._
import play.api.Logger
import play.api.http.Status._
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
  val detailsUri = "details"
  val knownFactsUri = "known-facts"
  val http: HttpGet with HttpPost = WSHttp
}

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def baseUri: String

  def registerUri: String

  def knownFactsUri: String

  def detailsUri: String

  def http: HttpGet with HttpPost


  def addKnownFacts(knownFacts: KnownFactsForService)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/${GovernmentGatewayConstants.KnownFactsAgentServiceName}/$knownFactsUri"""
    Logger.debug(s"[BusinessCustomerConnector][addKnownFacts] Call $postUrl")
    val jsonData = Json.toJson(knownFacts)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      auditAddKnownFactsCall(knownFacts, response)
      response
    }
  }


  def register(registerData: BusinessRegistrationRequest, service: String, isNonUKClientRegisteredByAgent: Boolean = false)
              (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[BusinessRegistrationResponse] = {
    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$registerUri"""
    Logger.debug(s"[BusinessCustomerConnector][register] Call $postUrl")
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

  def getDetails(identifier: String, identifierType: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val authLink = bcContext.user.authLink
    http.GET[HttpResponse](s"""$serviceUrl$authLink/$baseUri/$detailsUri/$identifier/$identifierType""")
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
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

  private def auditRegisterCall(
                                 input: BusinessRegistrationRequest,
                                 response: HttpResponse,
                                 service: String,
                                 isNonUKClientRegisteredByAgent: Boolean = false)
                               (implicit hc: HeaderCarrier) = {
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
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

}
