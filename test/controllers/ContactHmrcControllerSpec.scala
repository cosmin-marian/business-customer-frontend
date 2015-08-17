package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.{BusinessCustomerFrontendAuditConnector, ApplicationConfig, WSHttp}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, AnyContentAsFormUrlEncoded, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HttpResponse, SessionKeys}

import scala.concurrent.Future

class ContactHmrcControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = BusinessCustomerFrontendAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("business-customer-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
    reset(mockAuthConnector)
  }

  object TestContactHmrcController extends ContactHmrcController {
    implicit override val authConnector = mockAuthConnector
    override val httpPost = mockWSHttp
    override val contactFrontendPartialBaseUrl = ApplicationConfig.contactFrontendPartialBaseUrl
    override val contactFormServiceIdentifier = ApplicationConfig.contactFormServiceIdentifier
  }

  "ContactHmrcController" must {
    "use correct httpPost" in {
      ContactHmrcController.httpPost must be(WSHttp)
    }
    "use correct contactFrontendPartialBaseUrl && contactFormServiceIdentifier" in {
      ContactHmrcController.contactFrontendPartialBaseUrl must be(ApplicationConfig.contactFrontendPartialBaseUrl)
      ContactHmrcController.contactFormServiceIdentifier must be(ApplicationConfig.contactFormServiceIdentifier)
    }
    "contactHmrc respond with OK" in {
      contactHmrcWithAuthorised{
        result =>
          status(result) must be(OK)
      }
    }
    "contactHmrcThankYou respond with OK" in {
      contactHmrcThankYouWithAuthorised{
        result =>
          status(result) must be(OK)
      }
    }

    "submitContactHmrc" must {
      "respond with OK, for valid form" in {
        submitWithAuthorisedUserSuccess(FakeRequest().withFormUrlEncodedBody()) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }
      "respond with 400, for invalid form" in {
        submitWithAuthorisedUser400(FakeRequest().withFormUrlEncodedBody()) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }
      "respond with 500, for invalid form" in {
        submitWithAuthorisedUser500(FakeRequest().withFormUrlEncodedBody()) {
          result =>
            status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
      "respond with any other response status code, for invalid form" in {
        submitWithAuthorisedUser404(FakeRequest().withFormUrlEncodedBody()) {
          result =>
            val thrown = the[Exception] thrownBy await(result)
            thrown.getMessage must be("Unexpected status code from contact HMRC form: 404")
        }
      }
      "respond with 500, for no form data is there" in {
        submitWithAuthorisedUserNoForm(FakeRequest().withJsonBody(Json.toJson(""""""))) {
          result =>
            status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }

  def contactHmrcWithAuthorised(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestContactHmrcController.contactHmrc().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def contactHmrcThankYouWithAuthorised(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestContactHmrcController.contactHmrcThankYou().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
    val responseBody = Json.toJson("""""")
    when(mockWSHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(HttpResponse(OK, Some(responseBody))))

    val result = TestContactHmrcController.submitContactHmrc().apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserNoForm(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
    val responseBody = Json.toJson("""""")
    when(mockWSHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(HttpResponse(OK, Some(responseBody))))

    val result = TestContactHmrcController.submitContactHmrc().apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUser400(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
    val responseBody = Json.toJson("""""")
    when(mockWSHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(responseBody))))

    val result = TestContactHmrcController.submitContactHmrc().apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId).withCookies(new Cookie(HeaderNames.COOKIE, "COOKIE")))

    test(result)
  }

  def submitWithAuthorisedUser500(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
    val responseBody = Json.toJson("""""")
    when(mockWSHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(responseBody))))

    val result = TestContactHmrcController.submitContactHmrc().apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUser404(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
    when(mockWSHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))

    val result = TestContactHmrcController.submitContactHmrc().apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
