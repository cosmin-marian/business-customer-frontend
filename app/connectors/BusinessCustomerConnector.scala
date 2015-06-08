package connectors

import config.WSHttp
import models.{NonUKRegistrationResponse, NonUKRegistrationRequest}
import play.api.libs.json.{Reads, JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, HttpGet, HttpPost}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessCustomerConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("business-customer")
  val baseURI = "business-customer"
  val registerURI = "register"

  val http: HttpGet with HttpPost = WSHttp

  def responseTo[T](uri: String)(response: HttpResponse)(implicit rds: Reads[T]) = response.json.as[T]

  def registerNonUk(registerData: NonUKRegistrationRequest)(implicit headerCarrier: HeaderCarrier): Future[Option[NonUKRegistrationResponse]] = {
    val postUrl =  s"""$serviceURL/$baseURI/$registerURI"""
    val jsonData = Json.toJson(registerData)
    http.POST[JsValue, HttpResponse](postUrl, jsonData).map(responseTo[Option[NonUKRegistrationResponse]](postUrl))
  }

}

object BusinessCustomerConnector extends BusinessCustomerConnector
