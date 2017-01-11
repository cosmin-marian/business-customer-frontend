package connectors

import config.BusinessCustomerSessionCache
import models.{BusinessRegistration, Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class BusinessRegCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends BusinessRegCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
    override val sourceId: String = ""
  }

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(BusinessCustomerSessionCache)
      }

      "fetch saved BusinessDetails from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val businessReg: BusinessRegistration = BusinessRegistration("businessName", Address("addr1", "addr2", Some("addr3"), Some("addr4"), Some("postCode"), "GB"))

        when(mockSessionCache.fetchAndGetEntry[BusinessRegistration](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(businessReg)))
        val result = TestDataCacheConnector.fetchAndGetBusinessRegForSession
        await(result) must be(Some(businessReg))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val businessReg: BusinessRegistration = BusinessRegistration("businessName", Address("addr1", "addr2", Some("addr3"), Some("addr4"), Some("postCode"), "GB"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map(TestDataCacheConnector.sourceId -> Json.toJson(businessReg)))
        when(mockSessionCache.cache[ReviewDetails](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveBusinessRegDetails(businessReg)
        await(result).get must be(businessReg)
      }

    }
  }
}
