package connectors


import config.WSHttp
import models.MatchBusinessData
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

  def lookup(lookupData: MatchBusinessData)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$lookupURI"""
    http.POST( postUrl, Json.toJson(lookupData)) map {
      response =>
        response.status match {
          case OK | NOT_FOUND => Json.parse(response.body)
          case SERVICE_UNAVAILABLE => throw new ServiceUnavailableException("Service unavailable")
          case BAD_REQUEST => throw new BadRequestException("Bad Request")
          case INTERNAL_SERVER_ERROR => throw new InternalServerException("Internal server error")
          case _ => throw new RuntimeException("Unknown response")
        }
    }
  }
}

object BusinessMatchingConnector extends BusinessMatchingConnector
