package controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._


class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite {

  val request = FakeRequest()

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
        document.getElementById("sole-trader-first-name_field").text() must be("First Name")
        document.getElementById("sole-trader-last-name_field").text() must be("Last Name")
        document.getElementById("sole-trader-utr_field").text() must be("Self Assessment Unique Tax Reference")

      }

      "when selecting Limited Company option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("ltd-business-name_field").text() must be("Business Name")
          document.getElementById("ltd-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Unincorporated Body option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("uib-business-name_field").text() must be("Business Name")
          document.getElementById("uib-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Ordinary business partnership" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("obp-business-name_field").text() must be("Business Name")
          document.getElementById("obp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }

      "when selecting Limited Liability Partnership option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("llp-business-name_field").text() must be("Business Name")
          document.getElementById("llp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
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

        "if businessType is Sole Trader: FirstName, Surname and UTR" must {

          "not be empty" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("First Name must be entered")
            contentAsString(result) must include("Last Name must be entered")
            contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")

            document.getElementById("sole-trader-first-name_field").text() must be("First Name")
            document.getElementById("sole-trader-last-name_field").text() must be("Last Name")
            document.getElementById("sole-trader-utr_field").text() must be("Self Assessment Unique Tax Reference")
          }

          "if entered, First name must be less than 40 characters" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAFirstName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Maximum length is 40")
          }

          "if entered, Last name must be less than 40 characters" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sASurname" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Maximum length is 40")
          }

          "if entered, SA UTR must be 10 digits" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAUTR" -> "12345678917"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Unique Tax Reference must be 10 digits")
          }

          "if entered, SA UTR must be valid" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAUTR" -> "1234567892"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
          }
        }

        "if a Limited company: Business Name and COTAX UTR" must {

          "not be empty" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LTD"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Business Name must be entered")
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

            document.getElementById("ltd-business-name_field").text() must be("Business Name")
            document.getElementById("ltd-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
          }

          "if entered, Business Name must be less than 40 characters" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Maximum length is 40")
          }

          "if entered, COTAX UTR must be 10 digits" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdCotaxAUTR" -> "12345678917"))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Unique Tax Reference must be 10 digits")
          }

          "if entered, CO TAX UTR must be valid" in {
            val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdCotaxAUTR" -> "1234567892"))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
          }
        }
      }

      "if an Unincorporated body: Business Name and COTAX UTR" must {

        "not be empty" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "UIB"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

          document.getElementById("uib-business-name_field").text() must be("Business Name")
          document.getElementById("uib-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
        }

        "if entered, Business Name must be less than 40 characters" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Maximum length is 40")
        }

        "if entered, COTAX UTR must be 10 digits" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibCotaxAUTR" -> "12345678917"))
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include("Unique Tax Reference must be 10 digits")
        }

        "if entered, CO TAX UTR must be valid" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibCotaxAUTR" -> "1234567892"))
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
        }
      }


      "if an Ordinary business partnership: Business Name and Partnership Self Assessment UTR" must {

        "not be empty" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "OBP"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

          document.getElementById("obp-business-name_field").text() must be("Business Name")
          document.getElementById("obp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }

        "if entered, Business Name must be less than 40 characters" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Maximum length is 40")
        }

        "if entered, Partnership UTR must be 10 digits" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpPSAUTR" -> "12345678917"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Unique Tax Reference must be 10 digits")
        }

        "if entered, Partnership UTR must be valid" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpPSAUTR" -> "1234567892"))
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
        }
      }

      "if Limited liability partnership: Business Name and Partnership Self Assessment UTR" must {

        "not be empty" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LLP"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

          document.getElementById("llp-business-name_field").text() must be("Business Name")
          document.getElementById("llp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }

        "if entered, Business Name must be less than 40 characters" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Maximum length is 40")
        }

        "if entered, Partnership UTR must be 10 digits" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpPSAUTR" -> "12345678917"))
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Unique Tax Reference must be 10 digits")
        }

        "if entered, Partnership UTR must be valid" in {
          val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpPSAUTR" -> "1234567892"))
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
        }
      }

      "if valid text has been entered - continue to next action" in {
        val result = TestBusinessVerificationController.submit().apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "abc"}""")))
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/hello")
      }

      "if empty" must {

        "return BadRequest" in {
          val result = TestBusinessVerificationController.submit().apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : ""}""")))
          status(result) must be(BAD_REQUEST)
        }

      }
    }
  }
}
