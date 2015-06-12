package connectors

import config.WSHttp
import models.{NonUKRegistrationResponse, NonUKRegistrationRequest}
import play.api.i18n.Messages
import play.api.libs.json.{Reads, JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.http.Status._

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("business-customer")
  val baseURI = "business-customer"
  val registerURI = "register"

  val http: HttpGet with HttpPost = WSHttp


  def registerNonUk(registerData: NonUKRegistrationRequest)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[NonUKRegistrationResponse] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$registerURI"""
    val jsonData = Json.toJson(registerData)
    http.POST(postUrl, jsonData) map {
      response =>
        response.status match {
          case OK => response.json.as[NonUKRegistrationResponse]
          case NOT_FOUND => throw new InternalServerException(Messages("bc.connector.error.not-found"))
          case SERVICE_UNAVAILABLE => throw new ServiceUnavailableException(Messages("bc.connector.error.service-unavailable"))
          case BAD_REQUEST | INTERNAL_SERVER_ERROR => throw new InternalServerException(Messages("bc.connector.error.bad-request"))
          case status => throw new InternalServerException(Messages("bc.connector.error.unknown-response", status))
        }
    }
  }
}

object BusinessCustomerConnector extends BusinessCustomerConnector
