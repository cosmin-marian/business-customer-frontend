package controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationControllerSpec extends PlaySpec with OneServerPerSuite {
  val service = "ATED"

  "ApplicationController" must {

    "unauthorised" must {

      "respond with an OK" in {
        val result = controllers.ApplicationController.unauthorised().apply(FakeRequest())
        status(result) must equal(OK)
      }

      "load the unauthorised page" in {
        val result = controllers.ApplicationController.unauthorised().apply(FakeRequest())
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() must be(Messages("bc.unauthorised.title"))
      }

    }

    "Cancel" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }

    }

    "Not the right business link" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.logoutAndRedirectToHome(service).apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to login page" in {
        val result = controllers.ApplicationController.logoutAndRedirectToHome(service).apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/agent/ATED")
      }

    }

    "Logout" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.logout().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the logout page" in {
        val result = controllers.ApplicationController.logout().apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/signed-out")
      }

      "send to signed out page" in {
        val result = controllers.ApplicationController.signedOut().apply(FakeRequest())
        status(result) must be(OK)
      }

    }

    "Account summary link" must {
      "respond with a redirect" in {
        val result = controllers.ApplicationController.redirectToAgentSummary(service).apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the ated agent account summary page" in {
        val result = controllers.ApplicationController.redirectToAgentSummary(service).apply(FakeRequest())
        redirectLocation(result).get must include("/ated/summary?subscribed=true")
      }
    }
  }

}

