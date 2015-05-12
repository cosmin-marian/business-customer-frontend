package services

import connectors.{DataCacheConnector, BusinessMatchingConnector}
import models.{BusinessMatchDetails, ReviewDetails}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.domain.{CtUtr, SaUtr}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{CtAccount, Accounts, Authority, SaAccount}
import uk.gov.hmrc.play.frontend.auth.User

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestBusinessMatchingService extends BusinessMatchingService{
    override val businessMatchingConnector: BusinessMatchingConnector = TestConnector
    override val dataCacheConnector = TestDataCacheConnector
  }

  object TestConnector extends BusinessMatchingConnector {
    override def lookup(lookupData: BusinessMatchDetails)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = Future(reviewDetails)
  }

  object TestDataCacheConnector extends DataCacheConnector {
    override val sessionCache = SessionCache

    var writes: Int = 0

    override def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier) = {
      writes = writes + 1
      Future.successful(CacheMap(("Testdata"), Map("test" -> Json.toJson(1))))
    }

    def resetWrites = {
      writes = 0
    }
  }


  val reviewDetails: ReviewDetails = ReviewDetails("ACME", "UIB", "some address", "01234567890", "abc@def.com")
  val utr = "1234567890"
  val noMatchUtr = "9999999999"
  implicit val hc = HeaderCarrier()

  "BusinessMatchingService" must {

    "use the correct data cache connector" in {
      BusinessMatchingService.dataCacheConnector must be(DataCacheConnector)
    }

    "use the correct Middle service connector" in {
      BusinessMatchingService.businessMatchingConnector must be(BusinessMatchingConnector)
    }

    "accept SA User object and return ReviewDetails object" in {
      implicit val saUser = User("testuser", Authority(uri = "", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
      val result = TestBusinessMatchingService.matchBusiness
      await(result) must be(reviewDetails)
    }

    "accept CT User object and return ReviewDetails object" in {
      implicit val ctUser = User("testuser", Authority(uri = "", accounts = Accounts(ct = Some(CtAccount(s"/ct/individual/$utr", CtUtr(utr)))), None, None))
      val result = TestBusinessMatchingService.matchBusiness
      await(result) must be(reviewDetails)
    }
  }

  "BusinessMatchingService" must {
    "increment dataCache counter by 1" in {
      implicit val saUser = User("testuser", Authority(uri="",accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
      TestBusinessMatchingService.dataCacheConnector.resetWrites
      val result = TestBusinessMatchingService.matchBusiness
      await(result) must be(reviewDetails)

      TestBusinessMatchingService.dataCacheConnector.writes must be(1)
    }
  }

}
