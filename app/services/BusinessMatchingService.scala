package services

import connectors.BusinessMatchingConnector
import models.{BusinessMatchDetails, ReviewDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.User

object BusinessMatchingService extends BusinessMatchingService {
  override val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
}

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector

  def matchBusiness(implicit user: User, hc: HeaderCarrier): ReviewDetails = {
    // make match call with utr and boolean
    // store result in data cache

    val utr = getUserUtr
    val details = BusinessMatchDetails(true, utr.toString, None, None)
    businessMatchingConnector.lookup(details)

    ReviewDetails("ACME", "UIB", "some address", "01234567890", "abc@def.com")
  }

  def getUserUtr(implicit user: User) = {
    user.userAuthority.accounts.sa.getOrElse(user.userAuthority.accounts.ct.get.utr)
  }
}
