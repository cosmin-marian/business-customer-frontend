package controllers

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{SaUtr, Org}
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{SaAccount, Accounts, Authority, OrgAccount}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "AWRS"
  val utr = "1097172564"
  val mockAuthConnector = mock[AuthConnector]

  object TestHomeController extends HomeController{
    override val authConnector = mockAuthConnector
  }

  "HomeController" must {

    "respond to homePage" in {
      val result = route(FakeRequest(GET, "/business-customer/AWRS")).get
      status(result) must not be NOT_FOUND
    }

    "check for SA or COTAX enrolments" in {
      getWithAuthorisedUser {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.text() must include(utr)
      }
    }
  }




  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234"))), sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestHomeController.homePage(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
