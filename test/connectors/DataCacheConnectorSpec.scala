package connectors

import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends DataCacheConnector {
    val sessionCache: SessionCache = mockSessionCache
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(SessionCache)
      }

      "fetch saved BusinessDetails from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", "agent123")
        when(mockSessionCache.fetchAndGetEntry[ReviewDetails](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = TestDataCacheConnector.fetchAndGetBusinessDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", "agent123")
        val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))
        when(mockSessionCache.cache[ReviewDetails](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be(reviewDetails)
      }

    }

  }
}
