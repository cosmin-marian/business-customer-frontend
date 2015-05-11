package services

import connectors.{DataCacheConnector, BusinessMatchingConnector}
import models.{BusinessMatchDetails, ReviewDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def matchBusiness(implicit user: User, hc: HeaderCarrier): Future[ReviewDetails] = {

    val utr = getUserUtr
    val details = BusinessMatchDetails(true, utr.toString, None, None)
    val result = businessMatchingConnector.lookup(details)
    result map {
      case reviewData => dataCacheConnector.saveReviewDetails(reviewData)
    }
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 2" + result)
    result
  }

  def getUserUtr(implicit user: User) = {
    user.userAuthority.accounts.sa.getOrElse(user.userAuthority.accounts.ct.get.utr)
  }
}
