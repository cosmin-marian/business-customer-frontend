package controllers

import connectors.DataCacheConnector
import models.ReviewDetails
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite {

  def testReviewDetailsController = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Option(ReviewDetails("ACME", "Limited", "Address", "01234567890", "contact@acme.com")))
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

      val result = testReviewDetailsController.details().apply(FakeRequest())

      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text() must be("Welcome to ATED subscription")

      document.select("#business-name th:nth-child(1)").text() must be("Business name")
      document.select("#business-type th:nth-child(1)").text() must be("Type of business")
      document.select("#business-address th:nth-child(1)").text() must be("Business address")
      document.select("#business-telephone th:nth-child(1)").text() must be("Telephone")
      document.select("#business-email th:nth-child(1)").text() must be("Email")

      document.select("#business-name th:nth-child(2)").text() must be("ACME")
      document.select("#business-type th:nth-child(2)").text() must be("Limited")
      document.select("#business-address th:nth-child(2)").text() must be("Address")
      document.select("#business-telephone th:nth-child(2)").text() must be("01234567890")
      document.select("#business-email th:nth-child(2)").text() must be("contact@acme.com")

      document.select(".button").text() must be("Subscribe")
      document.select(".cancel-subscription-button").text() must be("Cancel Subscription")
      document.select(".nested-banner").text() must be("You are now ready to subscribe to ATED with the following business details. You can update your details on the following pages.")
    }

    "read existing business details data from cache (without updating data)" in {
      val testReviewController = testReviewDetailsController
      val result = testReviewController.details().apply(FakeRequest())

      status(result) must be(OK)

      testReviewController.dataCacheConnector.reads must be(1)
    }
  }

  "DataCacheConnector" must {
    "use the correct session cache" in {
      DataCacheConnector.sessionCache must be(SessionCache)
    }
  }
}
