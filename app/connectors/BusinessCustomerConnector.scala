package connectors

import audit.Auditable
import config.{WSHttpWithAudit, BusinessCustomerFrontendAuditConnector, WSHttp}
import models.{KnownFactsForService, BusinessRegistrationResponse, BusinessRegistrationRequest}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{EventTypes, Audit}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.{GovernmentGatewayConstants, AuthUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.http.Status._

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads with Auditable {

  lazy val serviceURL = baseUrl("business-customer")
  val baseURI = "business-customer"
  val registerURI = "register"
  val knownFactsURI = "known-facts"

  val http: HttpGet with HttpPost = WSHttpWithAudit


  def addKnownFacts(knownFacts: KnownFactsForService)(implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[HttpResponse]= {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/${GovernmentGatewayConstants.KNOWN_FACTS_AGENT_SERVICE_NAME}/$knownFactsURI"""
    Logger.debug(s"[BusinessCustomerConnector][addKnownFacts] Call $postUrl")
    val jsonData = Json.toJson(knownFacts)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)  map {
      response =>
        auditAddKnownFactsCall(knownFacts, response)
        response.status match {
          case OK => response
          case INTERNAL_SERVER_ERROR => {
            Logger.warn(s"[BusinessCustomerConnector][addKnownFacts] - Internal Server Error: ${response.body}")
            response
          }
          case status => {
            Logger.warn(s"[BusinessCustomerConnector][addKnownFacts] - $status Exception ${response.body}")
            throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)} Exception ${response.body}")
          }
        }
    }
  }


  def registerNonUk(registerData: BusinessRegistrationRequest)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessRegistrationResponse] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$registerURI"""
    Logger.debug(s"[BusinessCustomerConnector][registerNonUk] Call $postUrl")
    val jsonData = Json.toJson(registerData)
    http.POST(postUrl, jsonData) map {
      response =>
        auditRegisterNonUKCall(registerData, response)
        response.status match {
          case OK => response.json.as[BusinessRegistrationResponse]
          case NOT_FOUND => {
            Logger.warn(s"[BusinessCustomerConnector][registerNonUk] - Not Found Exception ${registerData.organisation.organisationName}")
            throw new InternalServerException(s"${Messages("bc.connector.error.not-found")}  Exception ${response.body}")
          }
          case SERVICE_UNAVAILABLE => {
            Logger.warn(s"[BusinessCustomerConnector][registerNonUk] - Service Unavailable Exception ${registerData.organisation.organisationName}")
            throw new ServiceUnavailableException(s"${Messages("bc.connector.error.service-unavailable")}  Exception ${response.body}")
          }
          case BAD_REQUEST | INTERNAL_SERVER_ERROR => {
            Logger.warn(s"[BusinessCustomerConnector][registerNonUk] - Bad Request Exception ${registerData.organisation.organisationName}")
            throw new InternalServerException(s"${Messages("bc.connector.error.bad-request")}  Exception ${response.body}")
          }
          case status => {
            Logger.warn(s"[BusinessCustomerConnector][registerNonUk] - $status Exception ${registerData.organisation.organisationName}")
            throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)}  Exception ${response.body}")
          }
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
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

  private def auditRegisterNonUKCall(input: BusinessRegistrationRequest, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "etmpRegisterNonUKCall",
      detail = Map("txName" -> "etmpRegisterNonUKCall",
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

object BusinessCustomerConnector extends BusinessCustomerConnector {
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}
