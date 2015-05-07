package controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationControllerSpec extends PlaySpec with OneServerPerSuite {

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
  }

}
