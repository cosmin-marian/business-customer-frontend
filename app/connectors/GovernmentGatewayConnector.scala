package connectors

import config.WSHttp
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GovernmentGatewayConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("government-gateway")

  val enrolURI = "enrol"
  val http: HttpGet with HttpPost = WSHttp

  def enrol(enrolRequest : EnrolRequest)(implicit headerCarrier: HeaderCarrier) :Future[EnrolResponse]= {
    val jsonData = Json.toJson(enrolRequest)
    val postUrl = s"""$serviceURL/$enrolURI"""
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map {
      response =>
        response.status match {
          case OK => response.json.as[EnrolResponse]
          case BAD_REQUEST => {
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"Bad Request Exception account Ref:${enrolRequest.knownFact}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new BadRequestException(response.body)
          }
          case NOT_FOUND => {
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"Not Found Exception account Ref:${enrolRequest.knownFact}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new NotFoundException(response.body)
          }
          case SERVICE_UNAVAILABLE => {
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"Service Unavailable Exception account Ref:${enrolRequest.knownFact}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new ServiceUnavailableException(response.body)
          }
          case status => {
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"status:${status} Exception account Ref:${enrolRequest.knownFact}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new InternalServerException(response.body)
          }
        }
    }

  }
}

object GovernmentGatewayConnector extends GovernmentGatewayConnector
