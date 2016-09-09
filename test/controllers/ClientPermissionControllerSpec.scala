package controllers

import java.util.UUID

import builders.SessionBuilder
import config.FrontendAuthConnector
import controllers.nonUkReg.ClientPermissionController
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class ClientPermissionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val service = "serviceName"

  object TestClientPermissionController extends ClientPermissionController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  "ClientPermissionController" must {

    "use correct DelegationConnector" in {
      ClientPermissionController.authConnector must be(FrontendAuthConnector)
    }

    "view" must {
      "redirect present user to client permission question page, if user is an agent" in {
        viewWithAuthorisedAgent(service) { result =>
          status(result) must be(OK)
          //redirectLocation(result) must be(Some(s"/business-customer/register/$service/ATED"))

           (Some("/business-customer/client-permission/ATED"))
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Do you have permission to register on behalf of your client?")
          document.getElementById("client-permission-header").text() must be("Do you have permission to register on behalf of your client?")
        }
      }

      "redirect to register non-uk page, if user is not an agent" in {
        viewWithAuthorisedClient(service) { result =>
          status(result) must be(SEE_OTHER)
           (Some(s"/business-customer/register/$service/ATED"))
        }
      }
    }

    "continue" must {
      "if user doesn't select any radio button, show form error with bad_request" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"permission": ""}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'yes', redirect it to business registration page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"permission": "true"}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/register/$service/NUK"))

        }
      }
      "if user select 'no', redirect it to view home page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"permission": "false"}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("#"))

        }
      }

    }
  }


  def viewWithAuthorisedAgent(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestClientPermissionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def viewWithAuthorisedClient(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestClientPermissionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def continueWithAuthorisedClient(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestClientPermissionController.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  def continueWithAuthorisedAgent(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestClientPermissionController.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }


}
