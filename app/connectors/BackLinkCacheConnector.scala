package connectors

import config.BusinessCustomerSessionCache
import models.BusinessRegistration
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BackLinkCacheConnector extends BackLinkCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_Back_Link"
}

trait BackLinkCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    sessionCache.fetchAndGetEntry[Map[String, Option[String]]](sourceId).map(_.flatMap(_.get(pageId)).flatten)
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[CacheMap] = {
    sessionCache.fetchAndGetEntry[Map[String, Option[String]]](sourceId).flatMap {
      oldLinksOpt =>
        oldLinksOpt match {
          case Some(oldLinks) =>
            sessionCache.cache[Map[String, Option[String]]](sourceId, oldLinks + (pageId -> returnUrl))
          case None =>
            sessionCache.cache[Map[String, Option[String]]](sourceId, Map(pageId -> returnUrl))
        }
    }
  }

}
