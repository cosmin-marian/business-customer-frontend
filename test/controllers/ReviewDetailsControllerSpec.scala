package controllers

import connectors.DataCacheConnector
import models.ReviewDetails
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestReviewDetailsController extends ReviewDetailsController {
    val dataCacheConnector = mockDataCacheConnector
  }

  "ReviewDetailsController" must {

    "use the correct data cache connector" in {
      controllers.ReviewDetailsController.dataCacheConnector must be(DataCacheConnector)
    }

    "return Review Details view" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", "some address", "01234567890", "abc@def.com")
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))

      val result = TestReviewDetailsController.businessDetails("ATED").apply(FakeRequest())

      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text must be("Welcome to ATED subscription")

      document.getElementById("business-name").text must be("ACME")
      document.getElementById("business-type").text must be("UIB")
      document.getElementById("business-address").text must be("some address")
      document.getElementById("business-telephone").text must be("01234567890")
      document.getElementById("business-email").text must be("abc@def.com")

      document.select(".button").text must be("Subscribe")
      document.select(".cancel-subscription-button").text must be("Cancel Subscription")
      document.select(".nested-banner").text must be("You are now ready to subscribe to ATED with the following business details. You can update your details on the following pages.")
    }

  }

}
