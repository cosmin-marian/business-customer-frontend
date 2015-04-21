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
    document.select("h1").text() must be("Welcome to ATED subscription")

    document.select("#business-name th:nth-child(1)").text() must be ("Business name")
    document.select("#business-type th:nth-child(1)").text() must be ("Type of business")
    document.select("#business-address th:nth-child(1)").text() must be ("Business address")
    document.select("#business-telephone th:nth-child(1)").text()must be ("Telephone")
    document.select("#business-email th:nth-child(1)").text() must be ("Email")

    document.select("#business-name th:nth-child(2)").text() must be ("ACME")
    document.select("#business-type th:nth-child(2)").text()  must be ("Limited")
    document.select("#business-address th:nth-child(2)").text() must be ("23 High Street Park View The Park Gloucester Gloucestershire ABC 123")
    document.select("#business-telephone th:nth-child(2)").text() must be ("01234567890")
    document.select("#business-email th:nth-child(2)").text() must be ("contact@acme.com")

    document.select(".button").text() must be ("Subscribe")
    document.select(".cancel-subscription-button").text() must be ("Cancel Subscription")
  }






}

