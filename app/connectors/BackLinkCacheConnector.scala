package connectors

import config.BusinessCustomerSessionCache
import play.api.libs.json.{Format, JsValue, Json}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BackLinkModel(backLinks: Map[String, Option[String]])

object BackLinkModel {
  implicit val formatter: Format[Map[String, Option[String]]] = {
    new Format[Map[String, Option[String]]] {
      def writes(m: Map[String, Option[String]]) = {
        Json.toJson(m.map {
          case (key, value) => key -> value
        })
      }

      def reads(json: JsValue) = {
        json.validate[Map[String, Option[String]]].map(_.map {
          case (key, value) => key -> value
        })
      }
    }
  }

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
    sessionCache.fetchAndGetEntry[BackLinkModel](sourceId).flatMap {
      oldLinksOpt =>
        oldLinksOpt match {
          case Some(oldLinks) =>
            sessionCache.cache[BackLinkModel](sourceId, BackLinkModel(oldLinks.backLinks + (pageId -> returnUrl)))
          case None =>
            sessionCache.cache[BackLinkModel](sourceId, BackLinkModel(Map(pageId -> returnUrl)))
        }
    }
  }

}
