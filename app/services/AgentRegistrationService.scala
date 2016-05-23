package services

import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import connectors.{BusinessCustomerConnector, DataCacheConnector, GovernmentGatewayConnector}
import models._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.{Logger, Play}
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, RunMode}
import utils.GovernmentGatewayConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AgentRegistrationService extends RunMode with Auditable {

  val governmentGatewayConnector: GovernmentGatewayConnector
  val dataCacheConnector: DataCacheConnector
  val businessCustomerConnector: BusinessCustomerConnector

  def enrolAgent(serviceName: String)(implicit bcContext: BusinessCustomerContext, headerCarrier: HeaderCarrier): Future[EnrolResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails)
      case _ => {
        Logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
      }
    }
  }

  private def enrolAgent(serviceName: String, businessDetails: ReviewDetails)
                        (implicit businessCustomerContext: BusinessCustomerContext, headerCarrier: HeaderCarrier): Future[EnrolResponse] = {
    for {
      _ <- businessCustomerConnector.addKnownFacts(createKnownFacts(businessDetails))
      enrolResponse <- governmentGatewayConnector.enrol(createEnrolRequest(serviceName, businessDetails))
    } yield {
      auditEnrolAgent(businessDetails, enrolResponse)
      enrolResponse
    }
  }

  private def createEnrolRequest(serviceName: String, businessDetails: ReviewDetails)(implicit bcContext: BusinessCustomerContext): EnrolRequest = {
    val agentEnrolmentService: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${serviceName.toLowerCase}.agentEnrolmentService")
    agentEnrolmentService match {
      case Some(enrolServiceName) => {
        val knownFactsList = List(businessDetails.agentReferenceNumber, Some(""), Some(""), Some(businessDetails.safeId)).flatten
        EnrolRequest(portalId = GovernmentGatewayConstants.PORTAL_IDENTIFIER,
          serviceName = enrolServiceName,
          friendlyName = GovernmentGatewayConstants.FRIENDLY_NAME,
          knownFacts = knownFactsList)
      }
      case _ => {
        Logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = ${serviceName}")
        throw new RuntimeException(Messages("bc.agent-service.error.no-agent-enrolment-service-name", serviceName, serviceName.toLowerCase))
      }
    }

  }

  private def createKnownFacts(businessDetails: ReviewDetails)(implicit bcContext: BusinessCustomerContext) = {
    val agentRefNo = businessDetails.agentReferenceNumber.getOrElse {
      Logger.warn(s"[AgentRegistrationService][createKnownFacts] - No Agent Reference Number Found")
      throw new RuntimeException(Messages("bc.agent-service.error.no-agent-reference", "[AgentRegistrationService][createKnownFacts]"))
    }
    val knownFacts = List(
      KnownFact(GovernmentGatewayConstants.KNOWN_FACTS_AGENT_REF_NO, agentRefNo),
      KnownFact(GovernmentGatewayConstants.KNOWN_FACTS_SAFEID, businessDetails.safeId)
    )
    KnownFactsForService(knownFacts)
  }

  private def auditEnrolAgent(businessDetails: ReviewDetails, enrolResponse: EnrolResponse)(implicit hc: HeaderCarrier) = {
    sendDataEvent("enrolAgent", detail = Map(
      "txName" -> "enrolAgent",
      "agentReferenceNumber" -> businessDetails.agentReferenceNumber.getOrElse(""),
      "service" -> enrolResponse.serviceName,
      "identifiers" -> enrolResponse.identifiers.toString()),
      eventType = EventTypes.Succeeded
    )
  }
}

object AgentRegistrationService extends AgentRegistrationService {
  override val governmentGatewayConnector = GovernmentGatewayConnector
  override val dataCacheConnector = DataCacheConnector
  override val businessCustomerConnector = BusinessCustomerConnector
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}

