package controllers


import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._


class BusinessRegControllerSpec extends PlaySpec with OneServerPerSuite {

  val request = FakeRequest()
  val service = "ATED"

  object TestBusinessRegController extends BusinessRegController {

  }

  "BusinessRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, "/business-customer/register")).get
      status(result) must not be (NOT_FOUND)
    }


    "return business registration view" in {

      val result = TestBusinessRegController.register().apply(FakeRequest())
      val document = Jsoup.parse(contentAsString(result))

      document.title() must be("Business Registration")
      document.getElementById("business-registration.header").text() must be("Add business details")
      document.getElementById("businessName_field").text() must be("Business name")
      document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
      document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
      document.getElementById("businessAddress.line_3_field").text() must be("Address line 3")
      document.getElementById("businessAddress.line_4_field").text() must be("Address line 4")
      document.getElementById("businessAddress.country_field").text() must be("Country")
      document.getElementById("submit").text() must be("Save and continue")
      document.getElementById("cancel").text() must be("Cancel")

    }

  }


  "send" must {

    "validate form" must {

      "not be empty" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)


        contentAsString(result) must include("Business name must be entered")
        contentAsString(result) must include("Address Line 1 must be entered")
        contentAsString(result) must include("Address Line 2 must be entered")
        contentAsString(result) must include("Country must be entered")
      }


      "If entered, Business name must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Business name must not be more than 40 characters")
      }

      "If entered, Address line 1 must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Address must not be more than 40 characters")
      }

      "If entered, Address line 2 must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Address must not be more than 40 characters")
      }


      "Address line 3 is optional but if entered, must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Address must not be more than 40 characters")
      }

      "Address line 4 is optional but if entered, must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Address must not be more than 40 characters")
      }

      "Country must be maximum of 40 characters" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"} }""")))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Country must not be more than 40 characters")
      }

      "If registration details entered are valid, save and continue button must redirect to review details page" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "ddd", "businessAddress": {"line_1": "ddd", "line_2": "ddd", "line_3": "", "line_4": "", "country": "England"} }""")))
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/review-details/ATED")

      }
    }
  }

}



