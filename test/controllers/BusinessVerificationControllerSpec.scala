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
        document.getElementById("sole-trader-first-name").text() must be("First Name")
        document.getElementById("sole-trader-last-name").text() must be("Last Name")
        document.getElementById("sole-trader-utr").text() must be("Self Assessment Unique Tax Reference")

      }

      "when selecting Limited Company option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("ltd-business-name").text() must be("Business Name")
          document.getElementById("ltd-cotax-utr").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Unincorporated Body option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("uib-business-name").text() must be("Business Name")
          document.getElementById("uib-cotax-utr").text() must be("COTAX Unique Tax Reference")
        }
      }

      "when selecting Ordinary business partnership" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("obp-business-name").text() must be("Business Name")
          document.getElementById("obp-psa-utr").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }

      "when selecting Limited Liability Partnership option" must {

        "add additional form fields to the screen for entry" in {
          val result = TestBusinessVerificationController.show().apply(FakeRequest())
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("llp-business-name").text() must be("Business Name")
          document.getElementById("llp-psa-utr").text() must be("Partnership Self Assessment Unique Tax Reference")
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

            contentAsString(result) must include("Please enter First name")
            contentAsString(result) must include("Please enter Surname")
            contentAsString(result) must include("Please enter SA UTR")

            document.getElementById("sole-trader-first-name").text() must be("First Name")
            document.getElementById("sole-trader-last-name").text() must be("Last Name")
            document.getElementById("sole-trader-utr").text() must be("Self Assessment Unique Tax Reference")
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

          "if businessType is Limited company: Business Name and COTAX UTR" must {

            "not be empty" in {
              val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LTD"))
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Please enter Business Name")
              contentAsString(result) must include("Please enter COTAX UTR")

              document.getElementById("ltd-business-name").text() must be("Business Name")
              document.getElementById("ltd-cotax-utr").text() must be("COTAX Unique Tax Reference")
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

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if businessType is Unincorporated body: Business Name and COTAX UTR" must {

            "not be empty" in {
              val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "UIB"))
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Please enter Business Name")
              contentAsString(result) must include("Please enter COTAX UTR")

              document.getElementById("uib-business-name").text() must be("Business Name")
              document.getElementById("uib-cotax-utr").text() must be("COTAX Unique Tax Reference")
            }

            "if entered, Business Name must be less than 40 characters" in {
              val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "SOP", "uibCompany.uibBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Maximum length is 40")
            }
          }


          "if businessType is Ordinary business partnership: Business Name and Partnership Self Assessment UTR" must {

            "not be empty" in {
              val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "OBP"))
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Please enter Business Name")
              contentAsString(result) must include("Please enter Partnership Self Assessment Unique Tax Reference")

              document.getElementById("obp-business-name").text() must be("Business Name")
              document.getElementById("obp-psa-utr").text() must be("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if businessType is Limited liability partnership: Business Name and Partnership Self Assessment UTR" must {

            "not be empty" in {
              val result = TestBusinessVerificationController.submit(request.withFormUrlEncodedBody("businessType" -> "LLP"))
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("Please enter Business Name")
              contentAsString(result) must include("Please enter Partnership Self Assessment Unique Tax Reference")

              document.getElementById("llp-business-name").text() must be("Business Name")
              document.getElementById("llp-psa-utr").text() must be("Partnership Self Assessment Unique Tax Reference")
            }
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
