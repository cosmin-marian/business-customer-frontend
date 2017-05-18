package connectors

import config.BusinessCustomerSessionCache
import models.{BusinessRegistration, ReviewDetails}
import play.api.libs.json.Format
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

  def fetchAndGetCachedDetails[T](formId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    sessionCache.fetchAndGetEntry[T](key = formId)

  def cacheDetails[T](formId: String, formData: T)(implicit hc: HeaderCarrier, formats: Format[T]): Future[T] = {
    sessionCache.cache[T](formId, formData).map(cacheMap => formData)
  }
}
