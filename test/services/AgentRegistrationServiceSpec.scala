package services

import builders.{AuthBuilder, TestAudit}
import connectors.{BusinessCustomerConnector, DataCacheConnector, GovernmentGatewayConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class AgentRegistrationServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val mockGGConnector = mock[GovernmentGatewayConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockBusinessCustomerConnector = mock[BusinessCustomerConnector]

  object TestAgentRegistrationService extends AgentRegistrationService {
    val governmentGatewayConnector: GovernmentGatewayConnector = mockGGConnector
    val dataCacheConnector = mockDataCacheConnector
    val businessCustomerConnector = mockBusinessCustomerConnector
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  "AgentRegistrationService" must {

    "use the correct connector" in {
      AgentRegistrationService.governmentGatewayConnector must be(GovernmentGatewayConnector)
    }

    "enrolAgent throw exception if we have no agent ref no" in {
      val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = None)

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockBusinessCustomerConnector.addKnownFacts(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
      when(mockGGConnector.enrol(Matchers.any())(Matchers.any())).thenReturn(Future.successful(enrolSuccessResponse))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("No Agent Reference Number Found")
    }

    "enrolAgent return the status if it worked" in {
      val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = Some("agent123"))

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockBusinessCustomerConnector.addKnownFacts(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
      when(mockGGConnector.enrol(Matchers.any())(Matchers.any())).thenReturn(Future.successful(enrolSuccessResponse))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      await(result) must be(enrolSuccessResponse)
    }

    "enrolAgent throw an exception if we have no details" in {
      val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(None))
      when(mockBusinessCustomerConnector.addKnownFacts(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("We could not find your details. Check and try again.")
    }

    "enrolAgent throw an exception if we have no service config" in {
      val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = Some("agent123"))

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockBusinessCustomerConnector.addKnownFacts(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
      val result = TestAgentRegistrationService.enrolAgent("INVALID_SERVICE_NAME")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must startWith("Agent Enrolment Service Name does not exist for")
    }
  }
}
