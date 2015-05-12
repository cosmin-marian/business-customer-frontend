package connectors

import java.util.UUID

import config.BusinessCustomerFrontendAuditConnector
import models.{Address, BusinessRegistration}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.{JsValue, Json}
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
      val successResponse = Json.parse( """{"businessName":"ACME","businessType":"Non UK-based Company","businessAddress":"111\nABC Street\nABC city\nABC 123\nABC"}""")
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))
      val businessRegistrationData = BusinessRegistration("ACME", businessAddress = Address("111", "ABC Street", Some("ABC city"), Some("ABC 123"), "ABC"))
      val result = TestBusinessCustomerConnector.register(businessRegistrationData)
      await(result).as[JsValue] must be(successResponse)
    }

  }

}
