package connectors


import java.util.UUID

import builders.TestAudit
import models.{BusinessCustomerContext, MatchBusinessData}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost {
    override val hooks: Seq[HttpHook] = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]
  implicit val user = mock[BusinessCustomerContext](RETURNS_DEEP_STUBS)


  object TestBusinessMatchingConnector extends BusinessMatchingConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
    override val lookupUri = "lookupUri"
    override val baseUri = "baseUri"
    override val serviceUrl = "serviceUrl"
  }

  val userType = "sa"
  val service = "ATED"

  override def beforeEach = {
    reset(mockWSHttp, user)
    when(user.user.authLink).thenReturn("/authLink")
  }

  "BusinessMatchingConnector" must {

    val matchSuccessResponse = Json.parse(
      """
        |{
        |  "businessName":"ACME",
        |  "businessType":"Unincorporated body",
        |  "businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123",
        |  "businessTelephone":"201234567890",
        |  "businessEmail":"contact@acme.com"
        |}
      """.stripMargin)

    val matchSuccessResponseInvalidJson =
      """{
        |  "sapNumber" : "111111111111",
        |  "safeId" : "XV111111111111",
        |  "isEditable" : false,
        |  "isAnAgent" : false,
        |  "isAnIndividual" : false,
        |  "organisation": {
        |    "organisationName" : "XYZ BREWERY CO LTD",
        |    "isAGroup" : false
        |  },
        |  "address": {
        |    "addressLine1" : "XYZ  ESTATE",
        |    "addressLine2" : "XYZ DRIVE",
        |    "addressLine3" : "XYZ",
        |    "addressLine4" : "XYZ",
        |    "postalCode" : "HU24 1ST",
        |    "countryCode" : "GB"
        |  },
        |  "contactDetails": {
        |    ,"mobileNumber" : "0121 812222"
        |  }
        |}
      """.stripMargin

    val matchSuccessResponseStripContactDetailsJson =
      """{
        |  "sapNumber" : "111111111111",
        |  "safeId" : "XV111111111111",
        |  "isEditable" : false,
        |  "isAnAgent" : false,
        |  "isAnIndividual" : false,
        |  "organisation": {
        |    "organisationName" : "XYZ BREWERY CO LTD",
        |    "isAGroup" : false
        |  },
        |  "address": {
        |    "addressLine1" : "XYZ  ESTATE",
        |    "addressLine2" : "XYZ DRIVE",
        |    "addressLine3" : "XYZ",
        |    "addressLine4" : "XYZ",
        |    "postalCode" : "HU24 1ST",
        |    "countryCode" : "GB"
        |  }
        |}
      """.stripMargin.replaceAll("[\r\n\t]", "")

    val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

    "for a successful match, return business details" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(matchSuccessResponse))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchSuccessResponse)
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "for a successful match with invalid JSON response, truncate contact details and return valid json" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val responseJson = HttpResponse(OK, responseString = Some(matchSuccessResponseInvalidJson))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(responseJson))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(Json.parse(matchSuccessResponseStripContactDetailsJson))
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "for unsuccessful match, return error message" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponse))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchFailureResponse)
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw service unavailable exception, if service is unavailable" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage must include("Service unavailable")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw bad request exception, if bad request is passed" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw internal server error, if Internal server error status is returned" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Internal server error")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw runtime exception, unknown status is returned" in {
      val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, "1111111111", false, false, None, None)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Unknown response")
      verify(mockWSHttp, times(1)).POST[MatchBusinessData, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
  }

}
