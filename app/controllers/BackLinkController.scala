package controllers

import connectors.BackLinkCacheConnector
import models.{BackLinkModel, BusinessCustomerContext}
import play.api.mvc.{AnyContent, Request, Call, Result}
import play.mvc.Http.Response
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BackLinkController extends BaseController {

  val controllerId: String
  val backLinkCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier) : Future[Option[String]] = {
    backLinkCacheConnector.saveBackLink(pageId, returnUrl)
  }

  def currentBackLink(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier):Future[Option[String]] = {
    backLinkCacheConnector.fetchAndGetBackLink(controllerId).map(_.getOrElse(BackLinkModel(None)).backLink)
  }

  def RedirectToExernal(redirectCall: String, backCall: Call)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier) = {
    Future.successful(Redirect(redirectCall))
  }

  def ForwardBackLinkToNextPage(nextPageId: String, redirectCall: Call)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      cache <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }

  def RedirectWithBackLink(nextPageId: String, redirectCall: Call, backCall: Call)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    for {
      cache <- setBackLink(nextPageId, Some(backCall.url))
    } yield{
      Redirect(redirectCall)
    }
  }
}
