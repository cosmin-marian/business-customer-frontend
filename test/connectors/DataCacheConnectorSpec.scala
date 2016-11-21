package connectors

import config.BusinessCustomerSessionCache
import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends DataCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
    override val sourceId: String = ""
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(BusinessCustomerSessionCache)
      }

      "fetch saved BusinessDetails from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))
        when(mockSessionCache.fetchAndGetEntry[ReviewDetails](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = TestDataCacheConnector.fetchAndGetBusinessDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map(TestDataCacheConnector.sourceId -> Json.toJson(reviewDetails)))
        when(mockSessionCache.cache[ReviewDetails](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be(reviewDetails)
      }

    }

    "clearCache" must {
      "clear the cache for the session" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.remove()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = TestDataCacheConnector.clearCache
        await(result).status must be(OK)
      }
    }
  }
}
