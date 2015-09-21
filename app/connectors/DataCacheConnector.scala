package connectors

import config.BusinessCustomerSessionCache
import models.ReviewDetails
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataCacheConnector extends DataCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
}

trait DataCacheConnector {

  val sessionCache: SessionCache

  val sourceId: String = "BC_Business_Details"

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    sessionCache.fetchAndGetEntry[ReviewDetails](sourceId)
  }

  def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    val result = sessionCache.cache[ReviewDetails](sourceId, reviewDetails)
    result flatMap {
      case data => Future.successful(data.getEntry[ReviewDetails](sourceId))
    }
  }
}
