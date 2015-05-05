package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
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
        contentAsString(result) must include("UNAUTHORISED")
      }

    }
  }
  
}
