package connectors


import audit.Auditable
import config.{WSHttpWithAudit, BusinessCustomerFrontendAuditConnector, WSHttp}
import models.MatchBusinessData
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{EventTypes, Audit}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig with RawResponseReads with Auditable {

  lazy val serviceURL = baseUrl("business-matching")
  val baseURI = "business-matching"
  val lookupURI = "business-lookup"

  val http: HttpGet with HttpPost = WSHttp

  def lookup(lookupData: MatchBusinessData, userType: String)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$lookupURI/${lookupData.utr}/$userType"""
    Logger.debug(s"[BusinessMatchingConnector][lookup] Call $postUrl")
    http.POST( postUrl, Json.toJson(lookupData)) map {
      response =>
        auditMatchCall(lookupData, userType, response)
        response.status match {
          case OK | NOT_FOUND => {
            Logger.info(s"[BusinessMatchingConnector][lookup] - postUrl = ${postUrl} && " +
              s"response.status = ${response.status} &&  response.body = ${response.body}")
            Json.parse(response.body)
          }
          case SERVICE_UNAVAILABLE => {
            Logger.warn(s"[BusinessMatchingConnector][lookup] - Service unavailableException ${lookupData.utr}")
            throw new ServiceUnavailableException("Service unavailable")
          }
          case BAD_REQUEST => {
            Logger.warn(s"[BusinessMatchingConnector][lookup] - Bad Request Exception ${lookupData.utr}")
            throw new BadRequestException("Bad Request")
          }
          case INTERNAL_SERVER_ERROR => {
            Logger.warn(s"[BusinessMatchingConnector][lookup] - Service Internal server error ${lookupData.utr}")
            throw new InternalServerException("Internal server error")
          }
          case status => {
            Logger.warn(s"[BusinessMatchingConnector][lookup] - $status Exception ${lookupData.utr}")
            throw new RuntimeException("Unknown response")
          }
        }
    }
  }

  private def auditMatchCall(input: MatchBusinessData, userType: String, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK | NOT_FOUND => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "etmpMatchCall",
        detail = Map("txName" -> "etmpMatchCall",
          "userType" -> s"${userType}",
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

object BusinessMatchingConnector extends BusinessMatchingConnector {
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}
