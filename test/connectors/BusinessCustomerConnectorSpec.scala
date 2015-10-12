package connectors

import java.util.UUID

import builders.{AuthBuilder, TestAudit}
import config.BusinessCustomerFrontendAuditConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector
    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  object TestBusinessCustomerConnector extends BusinessCustomerConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }
  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

  "BusinessCustomerConnector" must {
    val businessOrgData = EtmpOrganisation(organisationName = "testName")
    val etmpIdentification = EtmpIdentification(idNumber = "id1", issuingInstitution="HRMC", issuingCountryCode = "UK")
    val businessAddress = EtmpAddress("line1", "line2", None, None, None, "GB")

    val businessRequestData = BusinessRegistrationRequest(
      acknowledgmentReference = "SESS:123123123",
      organisation = businessOrgData,
      address = businessAddress,
      isAnAgent = false,
      isAGroup = false,
      identification = Some(etmpIdentification),
      contactDetails = EtmpContactDetails()
    )

    "addKnownFacts" must {
      "for successful knownFacts, return Response as HttpResponse" in {
        val knownFacts = KnownFactsForService(List(KnownFact("type", "value")))
        val successResponse = Json.toJson(knownFacts)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.addKnownFacts(knownFacts)
        await(result).status must be(OK)
        await(result).json must be(successResponse)
      }

      "for knownfacts Internal Server error, allow this through" in {
        val knownFacts = KnownFactsForService(List(KnownFact("type", "value")))
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.addKnownFacts(knownFacts)
        await(result).status must be(INTERNAL_SERVER_ERROR)
        await(result).json must be(matchFailureResponse)
      }

      "for knownFacts Unknown Status, throw an exception" in {
        val knownFacts = KnownFactsForService(List(KnownFact("type", "value")))
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(999, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.addKnownFacts(knownFacts)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown response status: 999")
      }
    }


    "registerNonUk" must {
      "for successful save, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
        await(result) must be(businessResponseData)
      }

      "for Service Unavailable, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service unavailable")
      }

      "for Not Found, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for InternalServerException, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Bad Request or Internal server error")
      }

      "for Unknown Error, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(999, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown response status: 999")
      }
    }
  }

}
