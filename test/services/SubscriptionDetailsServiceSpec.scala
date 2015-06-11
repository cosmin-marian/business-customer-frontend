package services

import connectors.DataCacheConnector
import models.SubscriptionDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.InternalServerException

import scala.concurrent.Future


class SubscriptionDetailsServiceSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestSubscriptionDetailsService extends SubscriptionDetailsService {
    val dataCacheConnector = mockDataCacheConnector
  }

  before {
    reset(mockDataCacheConnector)
  }
  "SubscriptionDetailsService" must {
    "use the correct data cache connector" in {
      BusinessMatchingService.dataCacheConnector must be(DataCacheConnector)
    }

    "Save the Subscription Details when we supply them" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val subscriptionDetails: SubscriptionDetails = SubscriptionDetails("ATED", true)
      when(mockDataCacheConnector.saveSubscriptionDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(subscriptionDetails)))
      val result = TestSubscriptionDetailsService.saveSubscriptionDetails(subscriptionDetails)
      await(result) must be(Some(subscriptionDetails))
    }

    "Fetch the Subscription Details when we have some" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val subscriptionDetails: SubscriptionDetails = SubscriptionDetails("ATED", true)
      when(mockDataCacheConnector.fetchSubscriptionDetails(Matchers.any())).thenReturn(Future.successful(Some(subscriptionDetails)))
      val result = TestSubscriptionDetailsService.fetchSubscriptionDetails
      await(result) must be(subscriptionDetails)
    }

    "Throw an exception when we have no subscription details to fetch" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(mockDataCacheConnector.fetchSubscriptionDetails(Matchers.any())).thenReturn(Future.successful(None))
      val result = TestSubscriptionDetailsService.fetchSubscriptionDetails

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("No Subscription Details found")
    }
  }
}
