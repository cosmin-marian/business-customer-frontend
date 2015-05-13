package connectors

import config.WSHttp
import models.BusinessRegistration
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessCustomerConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("business-customer")
  val baseURI = "business-customer"
  val registerURI = "register"

  val http: HttpGet with HttpPost = WSHttp

  def register(registerData: BusinessRegistration)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = {
    http.POST( s"""$serviceURL/$baseURI/$registerURI""", Json.toJson(registerData)).map {
      httpResponse =>
        Json.parse(httpResponse.body)
    }
  }

}

object BusinessCustomerConnector extends BusinessCustomerConnector
