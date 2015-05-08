package services

import connectors.{DataCacheConnector, BusinessMatchingConnector}
import models.{BusinessMatchDetails, ReviewDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.User
import scala.concurrent.Future

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def matchBusiness(implicit user: User, hc: HeaderCarrier): Future[ReviewDetails] = {
    // make match call with utr and boolean
    // store result in data cache

    val utr = getUserUtr
    val details = BusinessMatchDetails(true, utr.toString, None, None)
    businessMatchingConnector.lookup(details)

  }

  def getUserUtr(implicit user: User) = {
    user.userAuthority.accounts.sa.getOrElse(user.userAuthority.accounts.ct.get.utr)
  }
}
