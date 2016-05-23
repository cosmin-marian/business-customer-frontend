package controllers

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class NRLQuestionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  object TestNRLQuestionController extends NRLQuestionController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  "NRLQuestionController" must {
    "view" must {
      "redirect present user to NRL question page, if user is not an agent" in {

      }
      "redirect to register non-uk page, if user is an agent" in {

      }
    }
  }

  def viewWithAuthorisedUser(serviceName: String)(test: Future[Result] => Any) = {
      val sessionId = s"session-${UUID.randomUUID}"
      val userId = s"user-${UUID.randomUUID}"

      builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
      val result = TestNRLQuestionController.view(serviceName).apply(FakeRequest().withSession(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId))

      test(result)

  }



}
