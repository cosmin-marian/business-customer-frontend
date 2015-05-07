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
import uk.gov.hmrc.domain.{Org, Nino}
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{OrgAccount, PayeAccount, Accounts, Authority}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]

  object TestBusinessRegController extends BusinessRegController {
    override val authConnector = mockAuthConnector
  }

  val serviceName: String = "ATED"

  "BusinessRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName")).get
      status(result) must not be (NOT_FOUND)
    }

    "unauthorised users" must {
      "respond with a redirect" in {
        registerWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }

    "Authorised Users" must {

      "return business registration view" in {

        registerWithAuthorisedUser {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Business Registration")
            document.getElementById("business-registration.header").text() must be("Add business details")
            document.getElementById("business-name_field").text() must be("Business name")
            document.getElementById("address-line-1_field").text() must be("Address line 1")
            document.getElementById("address-line-2_field").text() must be("Address line 2")
            document.getElementById("address-line-3_field").text() must be("Address line 3")
            document.getElementById("address-line-4_field").text() must be("Address line 4")
            document.getElementById("country_field").text() must be("Country")
            document.getElementById("submit").text() must be("Save and continue")
            document.getElementById("cancel").text() must be("Cancel")
        }
      }
    }

  }

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }

    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
