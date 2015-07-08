package services

import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import connectors.{DataCacheConnector, GovernmentGatewayConnector}
import models.{EnrolResponse, EnrolRequest}
import play.api.{Play, Logger}
import play.api.i18n.Messages
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import utils.GovernmentGatewayConstants
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import uk.gov.hmrc.play.config.{AppName, RunMode}
import play.api.Play.current

trait AgentRegistrationService extends RunMode with Auditable {

  val governmentGatewayConnector: GovernmentGatewayConnector
  val dataCacheConnector: DataCacheConnector

  def enrolAgent(serviceName: String)(implicit headerCarrier: HeaderCarrier) :Future[EnrolResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails.agentReferenceNumber)
      case _ => {
        Logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
      }
    }
  }

  private def enrolAgent(serviceName: String, agentReferenceNumber: String)(implicit headerCarrier: HeaderCarrier) :Future[EnrolResponse] = {
    val enrolResponse = governmentGatewayConnector.enrol(createEnrolRequest(serviceName, agentReferenceNumber))
    auditEnrolAgent(agentReferenceNumber, enrolResponse)
    enrolResponse
  }

  private def createEnrolRequest(serviceName: String, agentReferenceNumber: String) :EnrolRequest = {
    val agentEnrolmentService: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${serviceName.toLowerCase}.agentEnrolmentService")
    agentEnrolmentService match {
      case Some(enrolServiceName) => {
        EnrolRequest(portalIdentifier = GovernmentGatewayConstants.PORTAL_IDENTIFIER,
          serviceName = enrolServiceName,
          friendlyName = GovernmentGatewayConstants.FRIENDLY_NAME,
          knownFact = agentReferenceNumber)
      }
      case _ => {
        Logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = ${serviceName}")
        throw new RuntimeException(Messages("bc.agent-service.error.no-agent-enrolment-service-name", serviceName, serviceName.toLowerCase))
      }
    }

  }

  private def auditEnrolAgent(agentReferenceNumber: String, enrolResponse: Future[EnrolResponse])(implicit hc: HeaderCarrier) = {
    enrolResponse.map { response =>
      sendDataEvent("enrolAgent", detail = Map(
        "agentReferenceNumber" -> agentReferenceNumber,
        "service" -> response.serviceName,
        "identifiersForDisplay" -> response.identifiersForDisplay,
        "friendlyName" -> response.friendlyName)
      )
    }
  }
}

object AgentRegistrationService extends AgentRegistrationService {
  override val governmentGatewayConnector = GovernmentGatewayConnector
  override val dataCacheConnector = DataCacheConnector
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}

