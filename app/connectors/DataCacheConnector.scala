package connectors

import models.ReviewDetails
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object DataCacheConnector extends DataCacheConnector {
  val sessionCache: SessionCache = SessionCache
}

trait DataCacheConnector {

  val sessionCache: SessionCache

  val sourceId: String = "BC_Business_Details"

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    sessionCache.fetchAndGetEntry[ReviewDetails](sourceId)
  }

  def saveBusinessDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier) = {
    sessionCache.cache[ReviewDetails](sourceId, reviewDetails) map {
      details =>
        details
    }
  }

}
