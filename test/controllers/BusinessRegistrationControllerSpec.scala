package controllers


import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._



class BusinessRegistrationControllerSpec extends PlaySpec with OneServerPerSuite{

  val request = FakeRequest()

  object TestBusinessRegController extends BusinessRegController {

  }

  "BusinessRegController" must {

    "respond to /" in {
      val result = route(FakeRequest(GET, "/business-customer/register")).get
      status(result) must not be (NOT_FOUND)
    }


   "return business registration view" in {

     val result = TestBusinessRegController.register().apply(FakeRequest())
     val document = Jsoup.parse(contentAsString(result))

     document.title() must be("Business Registration")
     document.getElementById("business-registration.header").text() must be("Add business details")
     document.getElementById("businessName_field").text() must be("Business name")
     document.getElementById("line_1_field").text() must be("Address line 1")
     document.getElementById("line_2_field").text() must be("Address line 2")
     document.getElementById("line_3_field").text() must be("Address line 3")
     document.getElementById("line_4_field").text() must be("Address line 4")
     document.getElementById("country_field").text() must be("Country")
     document.getElementById("submit").text() must be("Save and continue")
     document.getElementById("cancel").text() must be("Cancel")

   }

  }
}



