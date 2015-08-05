package connectors


import config.WSHttp
import models.MatchBusinessData
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("business-matching")
  val baseURI = "business-matching"
  val lookupURI = "business-lookup"

  val http: HttpGet with HttpPost = WSHttp

  def lookup(lookupData: MatchBusinessData, userType: String)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$lookupURI/${lookupData.utr}/$userType"""
    http.POST( postUrl, Json.toJson(lookupData)) map {
      response =>
        response.status match {
          case OK | NOT_FOUND => {
            Logger.info(s"[BusinessMatchingConnector][lookup] - response.status = ${response.status}   response.body = ${response.body}")
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

}

object BusinessMatchingConnector extends BusinessMatchingConnector
