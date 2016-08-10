package controllers

import java.util.UUID

import builders.SessionBuilder._
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


class NonUkClientRegistrationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val service = "serviceName"

  object TestNonUkClientRegistrationController extends NonUkClientRegistrationController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  "NonUkClientRegistrationController" must {

    "use correct Auth Connector" in {
      NonUkClientRegistrationController.authConnector must be(FrontendAuthConnector)
    }

    "view" must {
      "show the Non UK unique tax reference page" in {
        viewWithAuthorisedAgent(service) { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Does your client have a UK Unique Tax Reference for their business?")
          document.getElementById("nonuk-client-reg-subheader").text() must be("Add a client")
          document.getElementById("nonuk-client-reg-header").text() must be("Does your client have a UK Unique Tax Reference for their business?")

          document.getElementById("nonuk-client-reference-title").text() must be("Your agent reference is GG123456")

          document.getElementById("continue-btn").text() must be("Continue")
          document.getElementById("submit-btn").text() must be("View all my clients")
        }
      }
    }

    "continue" must {
      "if user doesn't select any radio button, show form error with bad_request" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"nUkUtr": ""}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("nUkUtr-error-0").text() must be("You must select an option")

        }
      }

      "if user select 'no', redirect to the next page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"nUkUtr": "false"}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9923/business-customer/next-page"))

        }
      }
      "if user select 'yes', redirect it to ATED home" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"nUkUtr": "true"}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9916/ated/home"))

        }
      }

    }
  }


  def viewWithAuthorisedAgent(serviceName: String)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNonUkClientRegistrationController.uniqueTaxReferenceView.apply(buildRequestWithSession(userId))
    test(result)
  }


  /*def viewWithAuthorisedClient(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestNonUkClientRegistrationController.uniqueTaxReferenceView.apply(buildRequestWithSession(userId))
    test(result)
  }*/


  def continueWithAuthorisedClient(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUkClientRegistrationController.uniqueTaxReferenceSubmit.apply(updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
