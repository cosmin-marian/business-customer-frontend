package controllers

import java.util.UUID

import builders.SessionBuilder
import config.FrontendAuthConnector
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


class NRLQuestionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val service = "serviceName"

  object TestNRLQuestionController extends NRLQuestionController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  "NRLQuestionController" must {

    "use correct DelegationConnector" in {
      NRLQuestionController.authConnector must be(FrontendAuthConnector)
    }

    "view" must {
      "redirect present user to NRL question page, if user is not an agent" in {
        viewWithAuthorisedClient(service) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Do you live outside of the UK for 6 months or more a year and receive rental income from the property?")
          document.select(".block-label").text() must include("Yes")
          document.select(".block-label").text() must include("No")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "redirect to register non-uk page, if user is an agent" in {
        viewWithAuthorisedAgent(service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/register/$service/NUK"))
        }
      }
    }

    "continue" must {
      "if user doesn't select any radio button, show form error with bad_request" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": ""}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'yes', redirect it to business verification page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": "true"}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/business-verification/$service/businessForm/SOP"))

        }
      }
      "if user select 'no', redirect it to business registration page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": "false"}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/register/$service/NUK"))

        }
      }

    }
  }


  def viewWithAuthorisedAgent(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNRLQuestionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def viewWithAuthorisedClient(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestNRLQuestionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def continueWithAuthorisedClient(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNRLQuestionController.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
