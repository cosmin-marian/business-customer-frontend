package controllers

import java.util.UUID

import builders.SessionBuilder
import config.FrontendAuthConnector
import controllers.nonUkReg.NRLQuestionControllerAgent
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


class NRLQuestionControllerAgentSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val service = "serviceName"

  object TestNRLQuestionControllerAgent extends NRLQuestionControllerAgent {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  "NRLQuestionControllerAgent" must {

    "use correct DelegationConnector" in {
      NRLQuestionControllerAgent.authConnector must be(FrontendAuthConnector)
    }

    "view" must {
      "redirect present user to NRL question page, if user is an agent" in {
        viewWithAuthorisedAgent(service) { result =>
          status(result) must be(OK)
          //redirectLocation(result) must be(Some(s"/business-customer/register/$service/ATED"))

           (Some("/business-customer/nrl-agent/ATED"))
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Are you a non-resident landlord?")
          document.getElementById("non-resident-text").text() must be("This is a non-resident landlord operating through an offshore company who pays tax through Self Assessment")
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
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": ""}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'yes', redirect it to business verification page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": "true"}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"#"))

        }
      }
      "if user select 'no', redirect it to business registration page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paysSA": "false"}"""))
        continueWithAuthorisedAgent(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/client-permission/ATED"))

        }
      }

    }
  }


  def viewWithAuthorisedAgent(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNRLQuestionControllerAgent.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def viewWithAuthorisedClient(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestNRLQuestionControllerAgent.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def continueWithAuthorisedClient(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNRLQuestionControllerAgent.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  def continueWithAuthorisedAgent(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestNRLQuestionControllerAgent.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
