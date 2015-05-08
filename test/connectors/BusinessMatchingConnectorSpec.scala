package connectors


import java.util.UUID

import config.BusinessCustomerFrontendAuditConnector
import models.{BusinessMatchDetails, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, Upstream4xxResponse}

import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector
    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  object TestBusinessMatchingConnector extends BusinessMatchingConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  before {
    reset(mockWSHttp)
  }

  "BusinessMatchingConnector" must {

    val successModel = ReviewDetails("ACME", "Unincorporated body", "23 High Street Park View The Park Gloucester Gloucestershire ABC 123", "201234567890", "contact@acme.com")


      "for a successful match, return business details" in {

        val businessDetails = BusinessMatchDetails(true, "1234567890", None, None)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessMatchDetails, ReviewDetails](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))
        val result = TestBusinessMatchingConnector.lookup(businessDetails)
        await(result) must be(successModel)
      }

      "for unsuccessful match, return error message" in {
        val businessDetails = BusinessMatchDetails(true, "1234567890", None, None)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessMatchDetails, ReviewDetails](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(Upstream4xxResponse("No business partner found", 404, 404)))
        val result = TestBusinessMatchingConnector.lookup(businessDetails)
        an[Upstream4xxResponse] must be thrownBy await(result)
      }
    }
}
