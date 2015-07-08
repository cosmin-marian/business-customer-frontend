package connectors


import java.util.UUID

import builders.AuthBuilder
import config.BusinessCustomerFrontendAuditConnector
import models.MatchBusinessData
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]
  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

  object TestBusinessMatchingConnector extends BusinessMatchingConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }
  val userType ="sa"

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "BusinessMatchingConnector" must {

    val matchSuccessResponse = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
    val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

    "for a successful match, return business details" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(matchSuccessResponse))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      await(result) must be(matchSuccessResponse)
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "for unsuccessful match, return error message" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponse))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      await(result) must be(matchFailureResponse)
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw service unavailable exception, if service is unavailable" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage must include("Service unavailable")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw bad request exception, if bad request is passed" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw internal server error, if Internal server error status is returned" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Internal server error")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw runtime exception, unknown status is returned" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Unknown response")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
  }
}
