package services

import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import connectors.{BusinessCustomerConnector, DataCacheConnector, GovernmentGatewayConnector}
import models.{KnownFactsForService, KnownFact, EnrolResponse, EnrolRequest}
import play.api.http.Status._
import play.api.{Play, Logger}
import play.api.i18n.Messages
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HttpResponse
import utils.GovernmentGatewayConstants
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import uk.gov.hmrc.play.config.{AppName, RunMode}
import play.api.Play.current

trait AgentRegistrationService extends RunMode with Auditable {

  val governmentGatewayConnector: GovernmentGatewayConnector
  val dataCacheConnector: DataCacheConnector
  val businessCustomerConnector: BusinessCustomerConnector

  def enrolAgent(serviceName: String)(implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[EnrolResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails.agentReferenceNumber)
      case _ => {
        Logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
      }
    }
  }

  private def enrolAgent(serviceName: String, agentReferenceNumber: String)
                        (implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[EnrolResponse] = {
    for {
      addKnowFactsResponse <- businessCustomerConnector.addKnownFacts(createKnownFacts(agentReferenceNumber))
      enrolResponse <- governmentGatewayConnector.enrol(createEnrolRequest(serviceName, agentReferenceNumber))
    } yield {
      auditEnrolAgent(agentReferenceNumber, enrolResponse)
      enrolResponse
    }
  }

  private def createEnrolRequest(serviceName: String, agentReferenceNumber: String) :EnrolRequest = {
    val agentEnrolmentService: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${serviceName.toLowerCase}.agentEnrolmentService")
    agentEnrolmentService match {
      case Some(enrolServiceName) => {
        EnrolRequest(portalId = GovernmentGatewayConstants.PORTAL_IDENTIFIER,
          serviceName = enrolServiceName,
          friendlyName = GovernmentGatewayConstants.FRIENDLY_NAME,
          knownFacts = List(agentReferenceNumber))
      }
      case _ => {
        Logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = ${serviceName}")
        throw new RuntimeException(Messages("bc.agent-service.error.no-agent-enrolment-service-name", serviceName, serviceName.toLowerCase))
      }
    }

  }

  private def createKnownFacts(agentReferenceNumber: String) = {
    val knownFacts = List(KnownFact(GovernmentGatewayConstants.AGENT_REFERENCE_NO_TYPE, agentReferenceNumber))
    KnownFactsForService(knownFacts)
  }

  private def auditEnrolAgent(agentReferenceNumber: String, enrolResponse: EnrolResponse)(implicit hc: HeaderCarrier) = {
    sendDataEvent("enrolAgent", detail = Map(
      "txName" -> "enrolAgent",
      "agentReferenceNumber" -> agentReferenceNumber,
      "service" -> enrolResponse.serviceName,
      "identifiers" -> enrolResponse.identifiers.toString())
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

