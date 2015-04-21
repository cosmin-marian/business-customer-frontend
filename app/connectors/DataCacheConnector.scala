package connectors

import models.ReviewDetails
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.Future

object DataCacheConnector extends DataCacheConnector

trait DataCacheConnector {

  val sourceId: String = "BC_Business_Details"

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    SessionCache.fetchAndGetEntry[ReviewDetails](sourceId)
  }
}

