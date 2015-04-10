package controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._


class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite {

  object TestBusinessVerificationController extends BusinessVerificationController

  "BusinessVerificationController" must {

    "respond to show" in {
      val result = route(FakeRequest(GET, "/business-customer-frontend/show")).get
      status(result) must not be (NOT_FOUND)
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer-frontend/hello")).get
      status(result) must not be (NOT_FOUND)
    }

    "show" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.show().apply(FakeRequest())
        status(result) must be(OK)
      }

      "return Business Verification view" in  {

        val result = TestBusinessVerificationController.show().apply(FakeRequest())

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("business-verification-header").text() must be("Business verification")
        document.getElementById("business-lookup").text() must be( "Business Lookup")
        document.select(".block-label").text() must include("Unincorporated Body")
        document.select(".block-label").text() must include("Limited Company")
        document.select(".block-label").text() must include("Sole Proprietor")
        document.select(".block-label").text() must include("Limited Liability Partnership")
        document.select(".block-label").text() must include("Partnership")
        document.select("button").text() must be("Continue")
      }

    }

    "hello" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.helloWorld().apply(FakeRequest())
        status(result) must be(OK)
      }

    }

    "submit" must {

      "validate form" must {

        "businessType" must {

          "be non-empty text" in {
            val result = TestBusinessVerificationController.submit().apply(FakeRequest().withJsonBody(Json.parse("""{"businessType" : "abc"}""")))
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer-frontend/hello")
          }
          "if empty" must {

            "return BadRequest" in {
              val result = TestBusinessVerificationController.submit().apply(FakeRequest().withJsonBody(Json.parse("""{"businessType" : ""}""")))
              status(result) must be(BAD_REQUEST)
            }

          }

        }

      }

    }

  }

}
