package connectors

import java.util.UUID

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
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector
    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  object TestBusinessCustomerConnector extends BusinessCustomerConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  "BusinessCustomerConnector" must {

    "for successful save, return ReviewDetails as Json" in {

      val businessOrgData = EtmpOrganisation(organisationName = "testName")
      val nonUKIdentification = NonUKIdentification(idNumber = "id1", issuingInstitution="HRMC", issuingCountryCode = "UK")
      val businessAddress = EtmpAddress("line1", "line2", None, None, None, "GB")

      val businessRequestData = NonUKRegistrationRequest(
        acknowledgmentReference = "SESS:123123123",
        organisation = businessOrgData,
        address = AddressChoice(foreignAddress = businessAddress),
        isAnAgent = false,
        isAGroup = false,
        nonUKIdentification = nonUKIdentification
      )

      val businessResponseData = NonUKRegistrationResponse(processingDate = "2015-01-01",
        sapNumber = "SAP123123",
        safeId = "SAFE123123",
        agentReferenceNumber = "AREF123123")

      val successResponse = Json.toJson(businessResponseData)

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val result = TestBusinessCustomerConnector.registerNonUk(businessRequestData)
      await(result) must be(Some(businessResponseData))
    }

  }

}
