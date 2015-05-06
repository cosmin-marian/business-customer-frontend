package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, SaAccount}


class HomeControllerSpec extends PlaySpec with OneServerPerSuite {

  val request = FakeRequest()
  val service = "AWRS"
  val utr = "1097172564"
  val user = AuthContext(authority = Authority(s"/auth/oid/userId", Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))

  object TestHomeController extends HomeController{
    val authConnector = null
  }

  "HomeController" must {

    "respond to homePage" in {
      val result = route(FakeRequest(GET, "/business-customer/AWRS")).get
      status(result) must not be NOT_FOUND
    }

    "check for SA or COTAX enrolments" in {
      val result = TestHomeController.homePage(service).apply(request)
      status(result) must be (OK)
    }
  }
}
