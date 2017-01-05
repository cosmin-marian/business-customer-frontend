package controllers.nonUKReg

import java.util.UUID

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class OverseasCompanyRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestController extends OverseasCompanyRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  val serviceName: String = "ATED"

  "OverseasCompanyRegController" must {

    "respond to /view" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/overseas/$serviceName")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /view & be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }

      "respond with a redirect for /send & be redirected to the unauthorised page" in {
        submitWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "Authorised Users" must {

      "return business registration view for a user for Non-UK" in {

        registerWithAuthorisedUser(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Enter your overseas business details")
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-registration-header").text() must be("Enter your overseas business details")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("hasOverseasTaxReference").text() must include("Do you have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }


      "return business registration view for an agent" in {

        registerWithAuthorisedAgent(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Enter your overseas agent details")
          document.getElementById("business-verification-text").text() must be("ATED agency set up")
          document.getElementById("business-registration-header").text() must be("Enter your overseas agent details")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("hasOverseasTaxReference").text() must include("Do you have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }
    }

  }

  def registerWithUnAuthorisedUser(businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedAgent(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestController.view(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestController.view(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestController.view(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
