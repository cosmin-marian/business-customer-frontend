package connectors

import config.BusinessCustomerSessionCache
import models.BackLinkModel
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.BusinessCustomerFeatureSwitches

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    if (BusinessCustomerFeatureSwitches.backLinks.enabled)
      sessionCache.fetchAndGetEntry[BackLinkModel](getKey(pageId)).map(_.flatMap(_.backLink))
    else
      Future.successful(None)
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    if (BusinessCustomerFeatureSwitches.backLinks.enabled)
      sessionCache.cache[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(cacheMap => returnUrl)
    else
      Future.successful(None)
  }

}