package connectors


import config.WSHttp
import models.MatchBusinessData
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("business-matching")
  val baseURI = "business-matching"
  val lookupURI = "business-lookup"

  val http: HttpGet with HttpPost = WSHttp

  def lookup(lookupData: MatchBusinessData)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = {
    http.POST( s"""$serviceURL/$baseURI/$lookupURI""", Json.toJson(lookupData)) map {
      response =>
        response.status match {
          case 200 | 404 => Json.toJson(response.body)
          case 503 => throw new ServiceUnavailableException("Service unavailable")
          case 400 | 500 => throw new InternalServerException("Bad Request or Internal server error")
          case _ => throw new InternalServerException("Unknown response")
        }
    }
  }
}

object BusinessMatchingConnector extends BusinessMatchingConnector
