package connectors

import audit.Auditable
import com.fasterxml.jackson.core.JsonParseException
import config.{BusinessCustomerFrontendAuditConnector, WSHttp}
import models.{BusinessCustomerContext, MatchBusinessData}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingConnector extends BusinessMatchingConnector {
  val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  val appName: String = AppName.appName
  val baseUri = "business-matching"
  val lookupUri = "business-lookup"
  val serviceUrl = baseUrl("business-matching")
  val http: HttpGet with HttpPost = WSHttp
}

trait BusinessMatchingConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def baseUri: String

  def lookupUri: String

  def http: HttpGet with HttpPost

  def lookup(lookupData: MatchBusinessData, userType: String, service: String)
            (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[JsValue] = {
    val authLink = bcContext.user.authLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$lookupUri/${lookupData.utr}/$userType"""
    Logger.debug(s"[BusinessMatchingConnector][lookup] Call $postUrl")
    http.POST(postUrl, Json.toJson(lookupData)) map { response =>
      auditMatchCall(lookupData, userType, response, service)
      response.status match {
        case OK | NOT_FOUND =>
          Logger.info(s"[BusinessMatchingConnector][lookup] - postUrl = $postUrl && " +
            s"response.status = ${response.status} &&  response.body = ${response.body}")
          //try catch added to handle JsonParseException in case ETMP/DES response with contact Details with ',' in it
          try {
            Json.parse(response.body)
          } catch {
            case jse: JsonParseException => truncateContactDetails(response.body)
          }
        ///////////////////// try catch end
        case SERVICE_UNAVAILABLE =>
          Logger.warn(s"[BusinessMatchingConnector][lookup] - Service unavailableException ${lookupData.utr}")
          throw new ServiceUnavailableException("Service unavailable")
        case BAD_REQUEST =>
          Logger.warn(s"[BusinessMatchingConnector][lookup] - Bad Request Exception ${lookupData.utr}")
          throw new BadRequestException("Bad Request")
        case INTERNAL_SERVER_ERROR =>
          Logger.warn(s"[BusinessMatchingConnector][lookup] - Service Internal server error ${lookupData.utr}")
          throw new InternalServerException("Internal server error")
        case status =>
          Logger.warn(s"[BusinessMatchingConnector][lookup] - $status Exception ${lookupData.utr}")
          throw new RuntimeException("Unknown response")
      }
    }
  }

  private def truncateContactDetails(responseJson: String): JsValue = {
    val replacedX1 = responseJson.replaceAll("[\r\n\t]", "")
    val removedContactDetails = replacedX1.substring(0, replacedX1.indexOf("contactDetails"))
    val correctedJsonString = removedContactDetails.substring(0, removedContactDetails.lastIndexOf(","))
    val validJson = correctedJsonString + "}"
    Json.parse(validJson)
  }

  private def auditMatchCall(input: MatchBusinessData, userType: String, response: HttpResponse, service: String)
                            (implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK | NOT_FOUND => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "etmpMatchCall",
      detail = Map("txName" -> "etmpMatchCall",
        "userType" -> s"$userType",
        "service" -> s"$service",
        "utr" -> input.utr,
        "requiresNameMatch" -> s"${input.requiresNameMatch}",
        "isAnAgent" -> s"${input.isAnAgent}",
        "individual" -> s"${input.individual}",
        "organisation" -> s"${input.organisation}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

}
