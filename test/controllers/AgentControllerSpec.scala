package controllers

import java.util.UUID

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{SubscriptionDetailsService, BusinessRegistrationService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class AgentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]
  val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  object TestAgentController extends AgentController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
    override val subscriptionDetailsService = mockSubscriptionDetailsService
  }

  val serviceName: String = "ATED"

  "AgentController" must {

    "respond to /agent/register" in {
      val result = route(FakeRequest(GET, s"/business-customer/agent/register/$serviceName")).get
      status(result) must not be (NOT_FOUND)
    }

    "unauthorised users" must {
      "respond with a redirect for /register" in {
        registerWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page for /register" in {
        registerWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }

    "Authorised Users" must {

      "return confirmation view" in {

        registerWithAuthorisedUser {
          result =>
            status(result) must be(OK)
        }
      }
    }
  }

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAgentController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockSubscriptionDetailsForAgent(serviceName, mockSubscriptionDetailsService)
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestAgentController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


}
