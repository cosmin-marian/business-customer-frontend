package connectors

import config.WSHttp
import models.{NonUKRegistrationResponse, NonUKRegistrationRequest}
import play.api.i18n.Messages
import play.api.libs.json.{Reads, JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessCustomerConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("business-customer")
  val baseURI = "business-customer"
  val registerURI = "register"

  val http: HttpGet with HttpPost = WSHttp

  def registerNonUk(registerData: NonUKRegistrationRequest)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = {
    val postUrl =  s"""$serviceURL/$baseURI/$registerURI"""
    val jsonData = Json.toJson(registerData)
    http.POST( postUrl, jsonData) map {
      response =>
        response.status match {
          case 200 | 404 => Json.parse(response.body)
          case 503 => throw new ServiceUnavailableException(Messages("bc.connector.error.service-unavailable"))
          case 400 | 500 => throw new InternalServerException(Messages("bc.connector.error.bad-request"))
          case status => throw new InternalServerException(Messages("bc.connector.error.unknown-response", status))
        }
    }
  }
}

object BusinessCustomerConnector extends BusinessCustomerConnector
