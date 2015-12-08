package controllers

import java.util.UUID

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
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
import models.{Address, ReviewDetails}


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
            document.getElementById("message").text() must be("You have successfully created your ATED account")
            document.getElementById("agent-reference").text() must startWith("Your agent reference is")
            document.getElementById("submit").text() must be("Finish and sign out")
            document.getElementById("confirm").text() must be("Your agent reference is also displayed in your account. You will not receive an email confirmation.")

            document.getElementById("what-happens-next").text() must be("What happens next")
            document.getElementById("what-happens-next-instruction").text() must startWith("You should give your clients your agent reference.")
            document.getElementById("happens-next-message1").text() must be("Your clients will then need to:")
            document.getElementById("happens-next-message2").text() must be("register their company for the new ATED service")
            document.getElementById("happens-next-message3").text() must be("create an agent request using the reference you gave them")
            document.getElementById("happens-next-message4").text() must startWith("let you know that they have completed this process")
        }
      }
      "return exception if no ARN present" in {

        val userId = s"user-${UUID.randomUUID}"

        builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
        when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(None))

        val result = TestAgentController.register(serviceName).apply(SessionBuilder.buildRequestWithSession(userId))
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("AgentReferenceNumber not found")
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
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val reviewDetails = ReviewDetails(businessName = "ABC",
      businessType = Some("corporate body"),
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
    when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))

    val result = TestAgentController.register(serviceName).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

}
