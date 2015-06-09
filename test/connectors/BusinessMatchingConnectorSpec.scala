package connectors


import config.BusinessCustomerFrontendAuditConnector
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}

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

    val matchSuccessResponse = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
    val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

      "for a successful match, return business details" in {
//        val businessDetails = BusinessMatchDetails(true, "1234567890", None, None)
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
//        when(mockWSHttp.POST[BusinessMatchDetails, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(matchSuccessResponse))))
//        val result = TestBusinessMatchingConnector.lookup(businessDetails)
//        await(result) must be(matchSuccessResponse)
      }

      "for unsuccessful match, return error message" in {
//        val businessDetails = BusinessMatchDetails(true, "1234567890", None, None)
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
//        when(mockWSHttp.POST[BusinessMatchDetails, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(matchFailureResponse))))
//        val result = TestBusinessMatchingConnector.lookup(businessDetails)
//        await(result).as[JsValue] must be(matchFailureResponse)
      }
    }
}
