package connectors

import _root_.metrics.MetricsEnum
import audit.Auditable
import config.{BusinessCustomerFrontendAuditConnector, WSHttp}
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._
import metrics.{MetricsEnum, Metrics}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GovernmentGatewayConnector extends ServicesConfig with RawResponseReads with Auditable {

  lazy val serviceURL = baseUrl("government-gateway")

  val enrolURI = "enrol"
  val http: HttpGet with HttpPost = WSHttp

  def metrics: Metrics

  def enrol(enrolRequest : EnrolRequest)(implicit headerCarrier: HeaderCarrier) :Future[EnrolResponse]= {
    val jsonData = Json.toJson(enrolRequest)
    val postUrl = s"""$serviceURL/$enrolURI"""

    val timerContext = metrics.startTimer(MetricsEnum.GG_AGENT_ENROL)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map {
      response =>
        val stopContext = timerContext.stop()
        auditEnrolCall(enrolRequest, response)
        response.status match {
          case OK =>
            metrics.incrementSuccessCounter(MetricsEnum.GG_AGENT_ENROL)
            response.json.as[EnrolResponse]
          case BAD_REQUEST => {
            metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"Bad Request Exception account Ref:${enrolRequest.knownFacts}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new BadRequestException(response.body)
          }
          case NOT_FOUND => {
            metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"Not Found Exception account Ref:${enrolRequest.knownFacts}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new NotFoundException(response.body)
          }
          case SERVICE_UNAVAILABLE => {
            metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"Service Unavailable Exception account Ref:${enrolRequest.knownFacts}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new ServiceUnavailableException(response.body)
          }
          case status => {
            metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
            Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
              s"gg url:${postUrl}, " +
              s"status:${status} Exception account Ref:${enrolRequest.knownFacts}, " +
              s"Service: ${enrolRequest.serviceName}}")
            throw new InternalServerException(response.body)
          }
        }
    }

  }

  private def auditEnrolCall(input: EnrolRequest, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "ggEnrolCall",
      detail = Map("txName" -> "ggEnrolCall",
        "friendlyName" -> s"${input.friendlyName}",
        "serviceName" -> s"${input.serviceName}",
        "portalId" -> s"${input.portalId}",
        "knownFacts" -> s"${input.knownFacts}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
  override def metrics = Metrics
}
