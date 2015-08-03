package connectors

import config.WSHttp
import models.{KnownFactsForService, NonUKRegistrationResponse, NonUKRegistrationRequest}
import play.api.Logger
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
  val knownFactsURI = "known-facts"

  val http: HttpGet with HttpPost = WSHttp


  def addKnownFacts(knownFacts: KnownFactsForService)(implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[HttpResponse]= {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$knownFactsURI"""
    val jsonData = Json.toJson(knownFacts)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)  map {
      response =>
        response.status match {
          case OK => response
          case status => {
            Logger.warn(s"[BusinessCustomerConnector][addKnownFacts] - $status Exception ${response.body}")
            throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)} Exception ${response.body}")
          }
        }
    }
  }


  def registerNonUk(registerData: NonUKRegistrationRequest)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[NonUKRegistrationResponse] = {
    val authLink = AuthUtils.getAuthLink()
    val postUrl = s"""$serviceURL$authLink/$baseURI/$registerURI"""
    val jsonData = Json.toJson(registerData)
    http.POST(postUrl, jsonData) map {
      response =>
        response.status match {
          case OK => response.json.as[NonUKRegistrationResponse]
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
}

object BusinessCustomerConnector extends BusinessCustomerConnector
