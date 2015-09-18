package controllers

import java.util.UUID

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future
import org.jsoup.Jsoup
import connectors.DataCacheConnector
import builders.{SessionBuilder, AuthBuilder}
import org.mockito.Mockito._
import play.api.mvc.Result
import org.mockito.Matchers
import services.AgentRegistrationService


class AgentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector =  mock[DataCacheConnector]


  object TestAgentController extends AgentController {
    override val authConnector = mockAuthConnector
    override  val dataCacheConnector = mockDataCacheConnector

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

      "return confirmation view for an agent" in {

        registerWithAuthorisedUser {
          result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Business Confirmation")
            document.getElementById("message").text() must be("You have successfully created your agent ATED account")
            document.getElementById("agent-reference").text() must startWith("Your agent reference is")
            document.getElementById("submit").text() must be("Finish and sign out")
            document.getElementById("confirm").text() must startWith("What happens next:")
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

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestAgentController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


//  def getWithAuthorisedAgent(test: Future[Result] => Any) {
//    val userId = s"user-${UUID.randomUUID}"
//    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
//
//
//    when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession.(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(s))
//
//    val result = TestAgentController.register().apply(SessionBuilder.buildRequestWithSession(userId))
//    test(result)
//  }



}
