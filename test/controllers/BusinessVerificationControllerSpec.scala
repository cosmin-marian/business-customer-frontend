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
      val result = route(FakeRequest(GET, "/business-customer/show")).get
      status(result) must not be (NOT_FOUND)
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer/hello")).get
      status(result) must not be (NOT_FOUND)
    }

    "show" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.show().apply(FakeRequest())
        status(result) must be(OK)
      }

      "return Business Verification view" in {

        val result = TestBusinessVerificationController.show().apply(FakeRequest())

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be("Business Verification")

        document.getElementById("business-verification-header").text() must be("Business verification")
        document.getElementById("business-lookup").text() must be("Business Lookup")
        document.select(".block-label").text() must include("Unincorporated Body")
        document.select(".block-label").text() must include("Limited Company")
        document.select(".block-label").text() must include("Sole Trader")
        document.select(".block-label").text() must include("Limited Liability Partnership")
        document.select(".block-label").text() must include("Partnership")
        document.select(".block-label").text() must include("Non UK-based Company")
        document.select("button").text() must be("Continue")
      }
    }
    "when selecting Sole Trader option" must {

      "add additional form fields to the screen for entry" in {
        val result = TestBusinessVerificationController.show().apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("soleTraderFirstName").text() must be("First Name")
        document.getElementById("soleTraderLastName").text() must be("Last Name")
        document.getElementById("soleTraderUniqueTaxReference").text() must be("Self Assessment Unique Tax Reference")

      }

      "when selecting Limited Company option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("ltdBusinessName").text() must be("Business Name")
          document.getElementById("ltdCOUTR").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Unincorporated Body option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("ltdBusinessName").text() must be("Business Name")
          document.getElementById("ltdCOUTR").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Ordinary business partnership" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("llpBusinessName").text() must be("Business Name")
          document.getElementById("llpCOUTR").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }

      "when selecting Limited Liability Partnership option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("llpBusinessName").text() must be("Business Name")
          document.getElementById("llpCOUTR").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
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
            redirectLocation(result).get must include("/business-customer/hello")
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
