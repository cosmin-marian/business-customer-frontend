package controllers

import connectors.BackLinkCacheConnector
import models.BusinessCustomerContext
import play.api.mvc.{Results, Result, Call}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BackLinkController extends BaseController {

  val controllerId: String
  val dataCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier) : Future[CacheMap] = {
    dataCacheConnector.saveBackLink(pageId, returnUrl)
  }

  def currentBackLink(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier):Future[Option[String]] = {
    dataCacheConnector.fetchAndGetBackLink(controllerId)
  }


  def ForwardWithBack(nextPageId: String, redirectCall: Call): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      cache <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }

  def RedirectWithBack(nextPageId: String, redirectCall: Call, backCall: Call): Future[Result] = {
    for {
      cache <- setBackLink(nextPageId, Some(backCall.url))
    } yield{
      Redirect(redirectCall)
    }
  }
}
