package services


import connectors.DataCacheConnector
import models.SubscriptionDetails
import play.api.i18n.Messages
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.InternalServerException
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
          case _ => throw new InternalServerException(Messages("bc.subscription.error.not-found"))
        }
    }
  }
}

object SubscriptionDetailsService extends SubscriptionDetailsService {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}