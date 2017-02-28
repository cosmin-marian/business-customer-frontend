package connectors

import config.BusinessCustomerSessionCache
import models.{BackLinkModel, Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.{BusinessCustomerFeatureSwitches, FeatureSwitch}

import scala.concurrent.Future

class BackLinkCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends BackLinkCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
    override val sourceId: String = ""
  }

  "BackLinkCacheConnector" must {

    "fetchAndGetBackLink" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(BusinessCustomerSessionCache)
      }

      "fetch saved BusinessDetails from SessionCache with Feature Switch on" in {
        val isFeatureEnable = BusinessCustomerFeatureSwitches.backLinks.enabled
        FeatureSwitch.enable(BusinessCustomerFeatureSwitches.backLinks)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))
        when(mockSessionCache.fetchAndGetEntry[BackLinkModel](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(backLink)))
        val result = TestDataCacheConnector.fetchAndGetBackLink("testPageId")
        await(result) must be(backLink.backLink)

        FeatureSwitch.setProp(BusinessCustomerFeatureSwitches.backLinks.name, isFeatureEnable)
      }

      "fetch saved BusinessDetails from SessionCache with Feature switch off" in {
        val isFeatureEnable = BusinessCustomerFeatureSwitches.backLinks.enabled
        FeatureSwitch.disable(BusinessCustomerFeatureSwitches.backLinks)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = TestDataCacheConnector.fetchAndGetBackLink("testPageId")
        await(result).isDefined must be(false)

        FeatureSwitch.setProp(BusinessCustomerFeatureSwitches.backLinks.name, isFeatureEnable)
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details with Feature Switch on" in {
        val isFeatureEnable = BusinessCustomerFeatureSwitches.backLinks.enabled
        FeatureSwitch.enable(BusinessCustomerFeatureSwitches.backLinks)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map(TestDataCacheConnector.sourceId -> Json.toJson(backLink)))
        when(mockSessionCache.cache[ReviewDetails](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveBackLink("testPageId", backLink.backLink)
        await(result) must be(backLink.backLink)

        FeatureSwitch.setProp(BusinessCustomerFeatureSwitches.backLinks.name, isFeatureEnable)
      }

      "save the fetched business details with Feature Switch off" in {
        val isFeatureEnable = BusinessCustomerFeatureSwitches.backLinks.enabled
        FeatureSwitch.disable(BusinessCustomerFeatureSwitches.backLinks)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = TestDataCacheConnector.saveBackLink("testPageId", Some("testBackLink"))
        await(result).isDefined must be(false)

        FeatureSwitch.setProp(BusinessCustomerFeatureSwitches.backLinks.name, isFeatureEnable)
      }
    }

  }
}