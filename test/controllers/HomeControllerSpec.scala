package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.{ReviewDetails, BusinessMatchDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.domain.{SaUtr, Org}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{SaAccount, Accounts, Authority, OrgAccount}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "AWRS"
  val utr = "1097172564"
  val noMatchUtr = "1111111111"
  val mockAuthConnector = mock[AuthConnector]

  object TestHomeController extends HomeController{
    override val businessMatchService: BusinessMatchingService = BusinessMatchingService
    override val authConnector = mockAuthConnector
  }

  object TestNoMatchHomeController extends HomeController{
    override val businessMatchService: BusinessMatchingService = TestBusinessMatchingService
    override val authConnector = mockAuthConnector
  }

  object TestBusinessMatchingService extends BusinessMatchingService{
    override val dataCacheConnector = DataCacheConnector
    override val businessMatchingConnector = TestBusinessMatchConnector
  }

  object TestBusinessMatchConnector extends BusinessMatchingConnector{
    override def lookup(lookupData: BusinessMatchDetails)(implicit headerCarrier: HeaderCarrier): Future[JsValue] = {
      Future.successful(throw new Exception("Something went wrong"))
    }
  }


  "HomeController" must {

    "respond to homePage" in {
      val result = route(FakeRequest(GET, "/business-customer/AWRS")).get
      status(result) must not be NOT_FOUND
    }

    "redirect to business verification page if the user has no SA or COTAX enrolments" in {
      getWithAuthorisedNoUtrUser (result =>  redirectLocation(result).get must include("/business-customer/business-verification"))

    }

    "check for SA or COTAX enrolments" in {
      getWithAuthorisedUser() {
        result =>
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }

    "redirect to Business Verification page if SA or COTAX enrolments find no match in ETMP" in {
      getWithAuthorisedUserNoMatch(authUtr = Some(noMatchUtr)) {
        result =>
          redirectLocation(result).get must not be(s"/business-customer/review-details/$service")
      }
    }
  }

  def getWithAuthorisedUser(authUtr: Option[String] = Some(utr))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234"))), sa = Some(SaAccount(s"/sa/individual/${authUtr.get}", SaUtr(authUtr.get)))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestHomeController.homePage(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def getWithAuthorisedUserNoMatch(authUtr: Option[String] = Some(noMatchUtr))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234"))), sa = Some(SaAccount(s"/sa/individual/${authUtr.get}", SaUtr(authUtr.get)))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestNoMatchHomeController.homePage(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def getWithAuthorisedNoUtrUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestHomeController.homePage(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
