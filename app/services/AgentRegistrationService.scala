package services

import connectors.{DataCacheConnector, GovernmentGatewayConnector}
import models.{EnrolResponse, EnrolRequest}
import play.api.{Play, Logger}
import play.api.i18n.Messages
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import utils.GovernmentGatewayConstants
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import uk.gov.hmrc.play.config.RunMode
import play.api.Play.current

trait AgentRegistrationService extends RunMode  {

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
    governmentGatewayConnector.enrol(createEnrolRequest(serviceName, agentReferenceNumber))
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
}

object AgentRegistrationService extends AgentRegistrationService {
  override val governmentGatewayConnector = GovernmentGatewayConnector
  override val dataCacheConnector = DataCacheConnector
}

