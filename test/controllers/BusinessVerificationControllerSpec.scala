package controllers

import java.util.UUID

import builders.AuthBuilder
import config.FrontendAuthConnector
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController {
    override val authConnector = mockAuthConnector
    override val businessMatchingService = mockBusinessMatchingService
  }

  "BusinessVerificationController" must {
    "use the correct connectors" in {
      BusinessVerificationController.authConnector must be(FrontendAuthConnector)
      BusinessVerificationController.businessMatchingService must be(BusinessMatchingService)
    }

    "respond to businessVerification" in {
      val result = route(FakeRequest(GET, s"/business-customer/business-verification/$service")).get
      status(result) must not be NOT_FOUND
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer/hello")).get
      status(result) must not be NOT_FOUND
    }

    "businessVerification" must {

      "authorised users" must {

        "respond with OK" in {
          businessVerificationWithAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "return Business Verification view for a user" in {

          businessVerificationWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Select your business type")
            document.getElementById("business-verification-text").text() must be("ATED registration")
            document.getElementById("business-verification-header").text() must be("Select your business type")
            document.select(".block-label").text() must include("Limited company")
            document.select(".block-label").text() must include("Limited liability partnership")
            document.select(".block-label").text() must include("partnership")
            document.select(".block-label").text() must include("I have an overseas company without a UK Unique Taxpayer Reference")
            document.select(".block-label").text() must include("Unit trust or collective investment vehicle")
            document.select(".block-label").text() must include("Limited partnership")
            document.select("button").text() must be("Continue")
          }
        }

        "return Business Verification view for an agent" in {

          businessVerificationWithAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Select a business type for your agency")
            document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
            document.getElementById("business-verification-agent-header").text() must be("Select a business type for your agency")
            document.select(".block-label").text() must include("Limited company")
            document.select(".block-label").text() must include("Sole trader / self-employed")
            document.select(".block-label").text() must include("Limited liability partnership")
            document.select(".block-label").text() must include("partnership")
            document.select(".block-label").text() must include("I have an overseas company without a UK Unique Taxpayer Reference")
            document.select(".block-label").text() must include("Limited partnership")
            document.select("button").text() must be("Continue")
          }
        }
      }


      "unauthorised users" must {
        "respond with a redirect & be redirected to the unauthorised page" in {
          businessVerificationWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }

      }
    }

    "continue" must {

      "selecting continue with no business type selected must display error message" in {
        continueWithAuthorisedUserJson("", FakeRequest().withJsonBody(Json.parse( """{"businessType" : ""}"""))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Please select a type of business")
        }
      }

      "if non-uk, continue to registration page" in {
        continueWithAuthorisedUserJson("NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NUK"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/nrl/$service")
        }
      }

      "if new, continue to NEW registration page" in {
        continueWithAuthorisedUserJson("NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NEW"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/register-gb/$service/NEW")
        }
      }

      "if group, continue to GROUP registration page" in {
        continueWithAuthorisedUserJson("NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "GROUP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/register-gb/$service/GROUP")
        }
      }

      "for any other option, redirect to home page again" in {
        continueWithAuthorisedUserJson("XYZ", FakeRequest().withJsonBody(Json.parse("""{"businessType" : "XYZ"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/agent/$service"))
        }
      }
    }

    "when selecting Sole Trader option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedSaUserJson("SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "SOP",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when SOP is selected for an Org user" in {
        continueWithAuthorisedUserJson("SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType" : "SOP",
            |  "isSaAccount": "false",
            |  "isOrgAccount": "true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an organisation with your Government Gateway ID. You cannot select Sole Trader/Self-employed as your business type. You need to have an individual Government Gateway ID and enrol for Self Assessment")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects SOP" in {
        continueWithAuthorisedSaOrgUserJson("SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "SOP",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry for ATED" in {
        businessLookupWithAuthorisedUser("SOP", "ATED") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your Self Assessment details")
          document.getElementById("business-type-paragraph-nrl").text() must be("As a non-resident landlord you pay tax through Self Assessment. Enter your Self Assessment details and we will attempt to match them against information we currently hold.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("saUTR_field").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("saUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }
      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("SOP", "AWRS") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("AWRS registration")
          document.getElementById("business-type-header").text() must be("Enter your Self Assessment details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("saUTR_field").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("saUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

      "display correct heading for AGENT selecting Sole Trader" in {
        businessLookupWithAuthorisedAgent("SOP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("saUTR_field").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("saUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }
    }

    "when selecting Limited Company option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("LTD", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LTD"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when LTD is selected for an Sa user" in {
        continueWithAuthorisedSaUserJson("LTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "LTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select Limited company/Partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects LTD" in {
        continueWithAuthorisedSaOrgUserJson("LTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "LTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("LTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("cotaxUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }

      "display correct heading for AGENT selecting Limited Company" in {
        businessLookupWithAuthorisedAgent("LTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("cotaxUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

    }

    "when selecting Unit Trust option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("UT", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UT"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when UT is selected for an Sa user" in {
        continueWithAuthorisedSaUserJson("UT", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "UT",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select Limited company/Partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects UT" in {
        continueWithAuthorisedSaOrgUserJson("UT", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "UT",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("UT") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("cotaxUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }

    }

    "when selecting Unincorporated Body option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("UIB", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UIB"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("UIB", "AWRS") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("AWRS registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("cotaxUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

      "display correct heading for AGENT selecting Unincorporated Association option" in {
        businessLookupWithAuthorisedAgent("UIB") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("cotaxUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Ordinary business partnership" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("OBP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "OBP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("OBP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Partnership name")
          document.getElementById("businessName_hint").text() must be("This is the name that you registered with HMRC")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

      "display correct heading for AGENT selecting Ordinary Business Partnership option" in {
        businessLookupWithAuthorisedAgent("OBP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Partnership name")
          document.getElementById("businessName_hint").text() must be("This is the name that you registered with HMRC")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Limited Liability Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("LLP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LLP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("LLP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

      "display correct heading for AGENT selecting Limited Liability Partnership option" in {
        businessLookupWithAuthorisedAgent("LLP") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Registered company name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Limited Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in {
        continueWithAuthorisedUserJson("LP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in {
        businessLookupWithAuthorisedUser("LP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-type-header").text() must be("Enter your business details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Partnership name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }

      "display correct heading for AGENT selecting Limited Partnership option" in {
        businessLookupWithAuthorisedAgent("LP") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("Enter your agency details")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementById("businessName_field").text() must include("Partnership name")
          document.getElementById("businessName_hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.getElementById("utr-help-question").text() must include("Where to find your UTR")
          document.getElementById("utr-help-questionAnswer").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.getElementById("psaUTR_hint").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as 'Tax Reference', 'UTR' or 'Official Use'.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

  }


  def businessVerificationWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessVerificationWithAuthorisedAgent(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessLookupWithAuthorisedUser(businessType: String, serviceName: String = service)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessForm(serviceName, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessVerificationWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUser(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedUserJson(businessType: String, fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedSaUserJson(businessType: String, fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedSaUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedSaOrgUserJson(businessType: String, fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedSaOrgUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def continueWithAuthorisedUser(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.continue(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def businessLookupWithAuthorisedAgent(businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.businessForm(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
