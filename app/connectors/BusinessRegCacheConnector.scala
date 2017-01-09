package connectors

import config.BusinessCustomerSessionCache
import models.{BusinessRegistration, ReviewDetails}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessRegCacheConnector extends BusinessRegCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_NonUK_Business_Details"
}

trait BusinessRegCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  def fetchAndGetBusinessRegForSession(implicit hc: HeaderCarrier): Future[Option[BusinessRegistration]] = sessionCache.fetchAndGetEntry[BusinessRegistration](sourceId)

  def saveBusinessRegDetails(businessReg: BusinessRegistration)(implicit hc: HeaderCarrier): Future[Option[BusinessRegistration]] = {
    val result = sessionCache.cache[BusinessRegistration](sourceId, businessReg)
    result flatMap {
      data => Future.successful(data.getEntry[BusinessRegistration](sourceId))
    }
  }
}
