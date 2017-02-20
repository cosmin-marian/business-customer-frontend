package connectors

import config.BusinessCustomerSessionCache
import models.BackLinkModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object BackLinkCacheConnector extends BackLinkCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_Back_Link"
}

trait BackLinkCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  private def getKey(pageId: String) = {
    s"$sourceId:$pageId"
  }
  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[BackLinkModel]] = {
    sessionCache.fetchAndGetEntry[BackLinkModel](getKey(pageId))
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    sessionCache.cache[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(cacheMap => returnUrl)
  }

}
