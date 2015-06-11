package services


import connectors.DataCacheConnector
import models.SubscriptionDetails
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait SubscriptionDetailsService {
  val dataCacheConnector: DataCacheConnector

  def saveSubscriptionDetails(subscriptionDetails: SubscriptionDetails)(implicit hc: HeaderCarrier): Future[Option[SubscriptionDetails]] = {
    dataCacheConnector.saveSubscriptionDetails(subscriptionDetails)
  }

  def fetchSubscriptionDetails(implicit hc: HeaderCarrier): Future[SubscriptionDetails] = {
    dataCacheConnector.fetchSubscriptionDetails map {
      cachedData =>
        cachedData match {
          case Some(subscriptionDetails) => subscriptionDetails
          case _ => throw new RuntimeException("No Subscription Details Found")
        }
    }
  }
}

object SubscriptionDetailsService extends SubscriptionDetailsService {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}