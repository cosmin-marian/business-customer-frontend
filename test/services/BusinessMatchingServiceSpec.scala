package services

import connectors.BusinessMatchingConnector
import models.ReviewDetails
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.domain.{CtUtr, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{CtAccount, Accounts, Authority, SaAccount}
import uk.gov.hmrc.play.frontend.auth.User

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite {

  object TestBusinessMatchingService extends BusinessMatchingService{
    override val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
}

  val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", "some address", "01234567890", "abc@def.com")

  val utr = "1234567890"
  implicit val hc = HeaderCarrier()

  "BusinessMatchingService" must {
    "accept User object and connect to BusinessMatchingConnector" in {

    }

    "accept SA User object and return ReviewDetails object" in {
      implicit val saUser = User("testuser", Authority(uri="",accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
      val result = TestBusinessMatchingService.matchBusiness
      result must be(reviewDetails)

    }

    "accept CT User object and return ReviewDetails object" in {
      implicit val ctUser = User("testuser", Authority(uri="",accounts = Accounts(ct = Some(CtAccount(s"/ct/individual/$utr", CtUtr(utr)))), None, None))
      val result = TestBusinessMatchingService.matchBusiness
      result must be(reviewDetails)

    }
  }

}
