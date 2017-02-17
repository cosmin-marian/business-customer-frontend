package connectors

import config.BusinessCustomerSessionCache
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BackLinkModel(backLinks: Map[String, Option[String]])

object BackLinkModel {
  implicit val formats = Json.format[BackLinkModel]
}

object BackLinkCacheConnector extends BackLinkCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_Back_Link"
}

trait BackLinkCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    sessionCache.fetchAndGetEntry[BackLinkModel](sourceId).map {
      _.flatMap(_.backLinks.get(pageId)).flatten
    }
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[CacheMap] = {
    sessionCache.fetchAndGetEntry[Map[String, Option[String]]](sourceId).flatMap {
      oldLinksOpt =>
        oldLinksOpt match {
          case Some(oldLinks) =>
            sessionCache.cache[BackLinkModel](sourceId, BackLinkModel(oldLinks + (pageId -> returnUrl)))
          case None =>
            sessionCache.cache[BackLinkModel](sourceId, BackLinkModel(Map(pageId -> returnUrl)))
        }
    }
  }

}
