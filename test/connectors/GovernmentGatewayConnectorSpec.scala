package connectors

import java.util.UUID

import builders.AuthBuilder
import config.BusinessCustomerFrontendAuditConnector
import models.{EnrolResponse, EnrolRequest}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

import scala.concurrent.Future


class GovernmentGatewayConnectorSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  object TestGovernmentGatewayConnector extends GovernmentGatewayConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "GovernmentGatewayConnector" must {
    val request = EnrolRequest(portalIdentifier = "ATED", serviceName = "ATED", friendlyName = "Main Enrolment", knownFact = "ATED-123")
    val response = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", friendlyName = "Main Enrolment",  identifiersForDisplay = "Ated_Ref_No")
    val successfulSubscribeJson = Json.toJson(response)
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val user = AuthBuilder.createUserAuthContext("User-Id", "name")

    "enrol user" must {
      "works for a user" in {

        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).
          thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(successfulSubscribeJson))))

        val result = TestGovernmentGatewayConnector.enrol(request)
        val enrolResponse = await(result)
        enrolResponse must be(response)
      }

      "return status as BAD_REQUEST, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[BadRequestException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status as NOT_FOUND, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[NotFoundException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status as SERVICE_UNAVAILABLE, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status anything else, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[InternalServerException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
    }
  }
}

