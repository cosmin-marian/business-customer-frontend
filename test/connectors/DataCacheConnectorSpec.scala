package connectors

import models.ReviewDetails
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends DataCacheConnector {
    val sessionCache: SessionCache = mockSessionCache
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "fetch saved BusinessDetails from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", "some address", "01234567890", "abc@def.com")
        when(mockSessionCache.fetchAndGetEntry[ReviewDetails](Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = TestDataCacheConnector.fetchAndGetBusinessDetailsForSession
        await(result) must be(Some(reviewDetails))
      }

    }

  }

}
