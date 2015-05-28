package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, Org}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import config.FrontendAuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController {
    override val businessMatchingConnector = mockBusinessMatchingConnector
    override val dataCacheConnector = mockDataCacheConnector
    override val authConnector = mockAuthConnector
  }

  "BusinessVerificationController" must {
    "use the correct authentication connector" in {
      controllers.BusinessVerificationController.authConnector must be(FrontendAuthConnector)
    }

    "use the correct business matching connector" in {
      controllers.BusinessVerificationController.businessMatchingConnector must be(BusinessMatchingConnector)
    }

    "use the correct data cache connector" in {
      controllers.BusinessVerificationController.dataCacheConnector must be(DataCacheConnector)
    }

    "respond to businessVerification" in {
      val result = route(FakeRequest(GET, "/business-customer/business-verification/ATED")).get
      status(result) must not be (NOT_FOUND)
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer/hello")).get
      status(result) must not be (NOT_FOUND)
    }

    "businessVerification" must {
      "authorised users" must {

        "respond with OK" in {

          businessVerificationWithAuthorisedUser {
            result =>
              status(result) must be(OK)
          }
        }

        "return Business Verification view" in {

          businessVerificationWithAuthorisedUser {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be("Business Verification")
              document.getElementById("business-verification-text").text() must be("ATED account registration")
              document.getElementById("business-lookup-text").text() must be("Before registering, you need to confirm your business details.")
              document.getElementById("business-verification-header").text() must be("About your business details")
              document.select(".block-label").text() must include("Unincorporated Association")
              document.select(".block-label").text() must include("Limited Company")
              document.select(".block-label").text() must include("Sole Trader / Self-employed")
              document.select(".block-label").text() must include("Limited Liability Partnership")
              document.select(".block-label").text() must include("Partnership")
              document.select(".block-label").text() must include("Non-UK Company")
              document.select(".block-label").text() must include("Limited Partnership")
              document.select("button").text() must be("Continue")
          }
        }
      }


      "unauthorised users" must {
        "respond with a redirect" in {
          businessVerificationWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          businessVerificationWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }
    }

    "when selecting Sole Trader option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("SOP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "SOP"}"""))) {
          result =>
            status(result) must be(303)
            redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }
    }

    "add additional form fields to the screen for entry" in {
      businessLookupWithAuthorisedUser("SOP") {
        result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("firstName_field").text() must be("First Name")
          document.getElementById("lastName_field").text() must be("Last Name")
          document.getElementById("saUTR_field").text() must be("Self Assessment Unique Tax Reference")
      }

    }
  }

  "when selecting Limited Company option" must {

    "redirect to next screen to allow additional form fields to be entered" in {
      continueWithAuthorisedUserJson("LTD", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LTD"}"""))) {
        result =>
          status(result) must be(303)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
      }
    }

    "add additional form fields to the screen for entry" in {
      businessLookupWithAuthorisedUser("LTD") {
        result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("businessName_field").text() must be("Business Name")
          document.getElementById("cotaxUTR_field").text() must be("COTAX Unique Tax Reference")
      }
    }
  }

  "when selecting Unincorporated Body option" must {

    "redirect to next screen to allow additional form fields to be entered" in {
      continueWithAuthorisedUserJson("UIB", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UIB"}"""))) {
        result =>
          status(result) must be(303)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
      }
    }

    "add additional form fields to the screen for entry" in {
      businessLookupWithAuthorisedUser("UIB") {
        result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("businessName_field").text() must be("Business Name")
          document.getElementById("cotaxUTR_field").text() must be("COTAX Unique Tax Reference")
      }
    }

    "when selecting Ordinary business partnership" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("OBP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "OBP"}"""))) {
          result =>
            status(result) must be(303)
            redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("OBP") {
          result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("businessName_field").text() must be("Business Name")
            document.getElementById("psaUTR_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }
    }

    "when selecting Limited Liability Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("LLP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LLP"}"""))) {
          result =>
            status(result) must be(303)
            redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("LLP") {
          result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("businessName_field").text() must be("Business Name")
            document.getElementById("psaUTR_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }
    }

    "when selecting Limited Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("LP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LLP"}"""))) {
          result =>
            status(result) must be(303)
            redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("LP") {
          result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("businessName_field").text() must be("Business Name")
            document.getElementById("psaUTR_field").text() must be("Partnership Self Assessment Unique Tax Reference")
        }
      }
    }

    "if empty" must {

      "return BadRequest" in {
        continueWithAuthorisedUserJson("", FakeRequest().withJsonBody(Json.parse( """{"businessType" : ""}"""))) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }
    }

    "if non-uk, continue to registration page" in {
      continueWithAuthorisedUserJson("NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NUK"}"""))) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/register")
      }
    }



    "hello" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.helloWorld("").apply(FakeRequest())
        status(result) must be(OK)
      }

    }

    "submit" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          continueWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          continueWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }


      "validate form" must {

        "if businessType is Sole Trader: FirstName, Surname and UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("SOP", FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "firstName" -> "", "lastName" -> "", "saUTR" -> "")) { result =>
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("First Name must be entered")
              contentAsString(result) must include("Last Name must be entered")
              contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")

              document.getElementById("firstName_field").text() must include("First Name")
              document.getElementById("lastName_field").text() must include("Last Name")
              document.getElementById("saUTR_field").text() must include("Self Assessment Unique Tax Reference")
            }
          }

          "if entered, First name must be less than 40 characters" in {
            submitWithAuthorisedUser("SOP", FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "firstName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("First Name must not be more than 40 characters")
            }
          }

          "if entered, Last name must be less than 40 characters" in {
            submitWithAuthorisedUser("SOP", FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "lastName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Last Name must not be more than 40 characters")
            }
          }

          "if entered, SA UTR must be 10 digits" in {
            submitWithAuthorisedUser("SOP", FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "saUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, SA UTR must be valid" in {
            submitWithAuthorisedUser("SOP", FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "saUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
            }
          }
        }

        "if a Limited company: Business Name and COTAX UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("LTD", FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "businessName" -> "", "cotaxUTR" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))

                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

                document.getElementById("businessName_field").text() must include("Business Name")
                document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser("LTD", FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "businessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Business Name must not be more than 40 characters")
            }
          }

          "if entered, COTAX UTR must be 10 digits" in {
            submitWithAuthorisedUser("LTD", FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "cotaxUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, CO TAX UTR must be valid" in {
            submitWithAuthorisedUser("LTD", FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "cotaxUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
            }
          }
        }

        "if an Unincorporated body: Business Name and COTAX UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("UIB", FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "businessName" -> "", "cotaxUTR" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

                document.getElementById("businessName_field").text() must include("Business Name")
                document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser("UIB", FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "businessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Business Name must not be more than 40 characters")
            }
          }

          "if entered, COTAX UTR must be 10 digits" in {
            submitWithAuthorisedUser("UIB", FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "cotaxUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, CO TAX UTR must be valid" in {
            submitWithAuthorisedUser("UIB", FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "cotaxUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
            }
          }
        }


        "if an Ordinary business partnership: Business Name and Partnership Self Assessment UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("OBP", FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "businessName" -> "", "psaUTR" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

                document.getElementById("businessName_field").text() must include("Business Name")
                document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser("OBP", FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "businessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Business Name must not be more than 40 characters")
            }
          }

          "if entered, Partnership UTR must be 10 digits" in {
            submitWithAuthorisedUser("OBP", FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "psaUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, Partnership UTR must be valid" in {
            submitWithAuthorisedUser("OBP", FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "psaUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
            }
          }
        }

        "if Limited liability partnership: Business Name and Partnership Self Assessment UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("LLP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "businessName" -> "", "psaUTR" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

                document.getElementById("businessName_field").text() must include("Business Name")
                document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser("LLP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "businessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Business Name must not be more than 40 characters")
            }
          }

          "if entered, Partnership UTR must be 10 digits" in {
            submitWithAuthorisedUser("LLP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "psaUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, Partnership UTR must be valid" in {
            submitWithAuthorisedUser("LLP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "psaUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
            }
          }
        }


        "if Limited partnership: Business Name and Partnership Self Assessment UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser("LP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LP", "businessName" -> "", "psaUTR" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

                document.getElementById("businessName_field").text() must include("Business Name")
                document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser("LP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LP", "businessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Business Name must not be more than 40 characters")
            }
          }

          "if entered, Partnership UTR must be 10 digits" in {
            submitWithAuthorisedUser("LP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LP", "psaUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, Partnership UTR must be valid" in {
            submitWithAuthorisedUser("LP", FakeRequest().withFormUrlEncodedBody("businessType" -> "LP", "psaUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
            }
          }
        }



        "submit of continue" must {

          "if valid text has been entered - continue to next action - UIB" in {
            val matchSuccessResponse = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), "U.K.")
            val successModel = ReviewDetails("ACME", "Unincorporated body", address)
            val inputJsonForUIB = Json.parse( """{ "businessType": "UIB", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
            when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(successModel)))

            continueWithAuthorisedUserJson("UIB", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/UIB")
            }
          }

          "if valid text has been entered - continue to next action - LTD" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJsonForUIB = Json.parse( """{ "businessType": "LTD", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            continueWithAuthorisedUserJson("LTD", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/LTD")
            }
          }

          "if valid text has been entered - continue to next action - SOP" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJsonForUIB = Json.parse( """{ "businessType": "SOP", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            continueWithAuthorisedUserJson("SOP", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/SOP")
            }
          }
          "if valid text has been entered - continue to next action - OBP" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJsonForUIB = Json.parse( """{ "businessType": "OBP", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            continueWithAuthorisedUserJson("OBP", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/OBP")
            }

          }

          "if valid text has been entered - continue to next action - LLP" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJsonForUIB = Json.parse( """{ "businessType": "LLP", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            continueWithAuthorisedUserJson("LLP", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/LLP")
            }

          }

          "if valid text has been entered - continue to next action - LP" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJsonForUIB = Json.parse( """{ "businessType": "LP", "uibCompany": {"businessName": "ACME", "cotaxUTR": "1111111111"} }""")

            continueWithAuthorisedUserJson("LP", FakeRequest().withJsonBody(inputJsonForUIB)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service/businessForm/LP")
            }

          }

          "if empty" must {

            "return BadRequest" in {
              continueWithAuthorisedUser("", FakeRequest().withFormUrlEncodedBody("businessType" -> "")) {
                result =>
                  status(result) must be(BAD_REQUEST)
              }
            }

          }
        }

        "if non-uk, continue to the registration page" in {
          continueWithAuthorisedUser("NUK", FakeRequest().withFormUrlEncodedBody("businessType" -> "NUK")) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/register/$service")
          }
        }

      }
    }
  }


  def businessVerificationWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessLookupWithAuthorisedUser(businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessForm(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessVerificationWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUser(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedUserJson(businessType: String, fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedUser(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP").withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
