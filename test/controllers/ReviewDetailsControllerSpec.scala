package controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite {

  object TestReviewDetailsController extends ReviewDetailsController

  "ReviewDetailsController" must {

    "respond to review details" in {
      val result = route(FakeRequest(GET, "/business-customer/review-details")).get
      status(result) must not be NOT_FOUND
    }
  }

  "ReviewDetailsController" must {

    "return 200 status code" in {
      val result = route(FakeRequest(GET, "/business-customer/review-details")).get
      status(result) must be (OK)
    }
  }

  "return Review Details view" in  {

    val result = TestReviewDetailsController.details().apply(FakeRequest())

    val document = Jsoup.parse(contentAsString(result))
    document.getElementById("business-review-details-header").text() must be("Welcome to ATED subscription")


    document.select(".table").toString must include ("Business name")
    document.select(".table").toString must include ("Type of business")
    document.select(".table").toString must include ("Business address")
    document.select(".table").toString must include ("Telephone")
    document.select(".table").toString must include ("Email")
    document.select(".table").toString must include ("ATED subscription")

    document.getElementById("business-name").text() must be ("ACME")
    document.getElementById("business-type").text() must be ("Limited")
    document.getElementById("business-address").text() must be ("23 High Street Park View The Park Gloucester Gloucestershire ABC 123")
    document.getElementById("business-telephone").text() must be ("Telephone")
    document.getElementById("business-email").text() must be ("01234567890")
    document.getElementById("subscribe-button").text() must be ("contact@acme.com")
  }


}

