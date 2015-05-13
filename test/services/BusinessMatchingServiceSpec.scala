package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.{BusinessMatchDetails, ReviewDetails}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsString, JsObject, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{CtUtr, SaUtr}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority, CtAccount, SaAccount}
import uk.gov.hmrc.play.frontend.auth.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestBusinessMatchingService extends BusinessMatchingService {
    override val businessMatchingConnector: BusinessMatchingConnector = TestConnector
    override val dataCacheConnector = TestDataCacheConnector
  }

  object TestBusinessMatchingServiceWithNoMatch extends BusinessMatchingService {
    override val businessMatchingConnector: BusinessMatchingConnector = TestConnectorWithNoMatch
    override val dataCacheConnector = TestDataCacheConnector
  }

  object TestConnector extends BusinessMatchingConnector {
    override def lookup(lookupData: BusinessMatchDetails)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = Future(Json.toJson(reviewDetails))
  }

  object TestConnectorWithNoMatch extends BusinessMatchingConnector {
    override def lookup(lookupData: BusinessMatchDetails)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = Future(Json.toJson(JsObject(Seq("error" -> JsString("Generic error")))))
  }

  object TestDataCacheConnector extends DataCacheConnector {
    override val sessionCache = SessionCache

    var writes: Int = 0

    override def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier) = {
      writes = writes + 1
      Future.successful(Some(reviewDetails))
    }

    def resetWrites = {
      writes = 0
    }
  }


  val reviewDetails = ReviewDetails("ACME", "UIB", "some address")
  val reviewDetailsJson = Json.toJson(reviewDetails)
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
      await(result) must be(reviewDetailsJson)
    }

    "accept CT User object and return ReviewDetails object" in {
      implicit val ctUser = User("testuser", Authority(uri = "", accounts = Accounts(ct = Some(CtAccount(s"/ct/individual/$utr", CtUtr(utr)))), None, None))
      val result = TestBusinessMatchingService.matchBusiness
      await(result) must be(reviewDetailsJson)
    }

    "return error when no match is found" in {
      implicit val ctUser = User("testuser", Authority(uri = "", accounts = Accounts(ct = Some(CtAccount(s"/ct/individual/$utr", CtUtr(utr)))), None, None))
      val result = TestBusinessMatchingServiceWithNoMatch.matchBusiness
      await(result) must be(JsObject(Seq("error" -> JsString("Generic error"))))
    }
  }

  "BusinessMatchingService" must {
    "increment dataCache counter by 1" in {
      implicit val saUser = User("testuser", Authority(uri="",accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
      TestBusinessMatchingService.dataCacheConnector.resetWrites
      val result = TestBusinessMatchingService.matchBusiness

      await(result) must be(reviewDetailsJson)

      TestBusinessMatchingService.dataCacheConnector.writes must be(1)
    }
  }

}
