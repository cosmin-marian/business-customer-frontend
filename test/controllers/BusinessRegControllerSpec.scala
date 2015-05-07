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
      document.getElementById("business-name_field").text() must be("Business name")
      document.getElementById("address-line-1_field").text() must be("Address line 1")
      document.getElementById("address-line-2_field").text() must be("Address line 2")
      document.getElementById("address-line-3_field").text() must be("Address line 3")
      document.getElementById("address-line-4_field").text() must be("Address line 4")
      document.getElementById("country_field").text() must be("Country")
      document.getElementById("submit").text() must be("Save and continue")
      document.getElementById("cancel").text() must be("Cancel")

    }

  }


  "send" must {

    "validate form" must {

      "not be empty" in {
        //val someJson = Json.parse( """{ "businessName": "ACME", "businessAddress": {"line_1": "111", "line_2": "ABC Street", "line_3": "ABC city", "line_4": "ABC 123", "country": "ABC"} }""")
        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)


        contentAsString(result) must include("Business name must be entered")
        contentAsString(result) must include("Address Line 1 must be entered")
        contentAsString(result) must include("Address Line 2 must be entered")
        contentAsString(result) must include("Country must be entered")
      }

      /*
      "If entered, characters must be maximum of 40" in {

        val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse("""{ "businessName": "egg", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include("Business name must not be more than 40 characters")
      }*/
    }
  }

}
