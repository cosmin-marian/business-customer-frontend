package controllers

import connectors.DataCacheConnector
import models.ReviewDetails
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite {

  object TestReviewDetailsController extends ReviewDetailsController {
    val mockDataCacheConnector: DataCacheConnector = new DataCacheConnector {
      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Option(ReviewDetails("", "", "", "", "")))
      }
    }
    override val dataCacheConnector = mockDataCacheConnector
  }

  "ReviewDetailsController" must {

    "respond to review details" in {
      val result = route(FakeRequest(GET, "/business-customer/review-details")).get
      status(result) must not be NOT_FOUND
    }
  }

  "ReviewDetailsController" must {

    "return 200 status code" in {
      val result = route(FakeRequest(GET, "/business-customer/review-details")).get
      status(result) must be(OK)
    }
  }

  "return Review Details view" in {

    val result = TestReviewDetailsController.details().apply(FakeRequest())

    val document = Jsoup.parse(contentAsString(result))
    document.select("h1").text() must be("Welcome to ATED subscription")

    document.select("#business-name th:nth-child(1)").text() must be("Business name")
    document.select("#business-type th:nth-child(1)").text() must be("Type of business")
    document.select("#business-address th:nth-child(1)").text() must be("Business address")
    document.select("#business-telephone th:nth-child(1)").text() must be("Telephone")
    document.select("#business-email th:nth-child(1)").text() must be("Email")

    document.select("#business-name th:nth-child(2)").text() must be("ACME")
    document.select("#business-type th:nth-child(2)").text() must be("Limited")
    document.select("#business-address th:nth-child(2)").text() must be("23 High Street Park View The Park Gloucester Gloucestershire ABC 123")
    document.select("#business-telephone th:nth-child(2)").text() must be("01234567890")
    document.select("#business-email th:nth-child(2)").text() must be("contact@acme.com")

    document.select(".button").text() must be("Subscribe")
    document.select(".cancel-subscription-button").text() must be("Cancel Subscription")
    document.select(".nested-banner").text() must be("You are now ready to subscribe to ATED with the following business details. You can update some of these details on the next page.")
  }

  "read existing business details data from cache (without updating data)" in {
    val result = TestReviewDetailsController.details().apply(FakeRequest())
    status(result) must be (OK)

    reviewDetailsController.dataCacheConnector.reads must be (1)

  }

    val mockDataCacheConnector = new DataCacheConnector {
      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Option(ReviewDetails("", "", "", "", "")))
      }
    }

    val reviewDetailsController = new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
    }

}

