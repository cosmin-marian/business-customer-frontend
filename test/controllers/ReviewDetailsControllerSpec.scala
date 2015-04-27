package controllers

import connectors.DataCacheConnector
import models.ReviewDetails
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite {

  def testReviewDetailsController = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(ReviewDetails("ACME", "Limited", "Address", "01234567890", "contact@acme.com")))
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
    }
  }

  "ReviewDetailsController" must {

    "use the correct data cache connector" in {
      controllers.ReviewDetailsController.dataCacheConnector must be(DataCacheConnector)
    }

    "return Review Details view" in {

      val result = testReviewDetailsController.businessDetails("ATED").apply(FakeRequest())

      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text must be("Welcome to ATED subscription")

      document.getElementById("business-name").text must be("ACME")
      document.getElementById("business-type").text must be("Limited")
      document.getElementById("business-address").text must be("Address")
      document.getElementById("business-telephone").text must be("01234567890")
      document.getElementById("business-email").text must be("contact@acme.com")

      document.select(".button").text must be("Subscribe")
      document.select(".cancel-subscription-button").text must be("Cancel Subscription")
      document.select(".nested-banner").text must be("You are now ready to subscribe to ATED with the following business details. You can update your details on the following pages.")
    }

    "read existing business details data from cache (without updating data)" in {
      val testReviewController = testReviewDetailsController
      val result = testReviewController.businessDetails("ATED").apply(FakeRequest())

      status(result) must be(OK)

      testReviewController.dataCacheConnector.reads must be(1)
    }
  }
}
