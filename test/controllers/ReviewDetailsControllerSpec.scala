package controllers

import java.util.UUID

import connectors.DataCacheConnector
import models.ReviewDetails
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Org, Nino}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{OrgAccount, PayeAccount, Accounts, Authority}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]

  def testReviewDetailsController = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(ReviewDetails("ACME", "Limited", "Address")))
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector

      override val authConnector = mockAuthConnector
    }
  }

  "ReviewDetailsController" must {

    "use the correct data cache connector" in {
      controllers.ReviewDetailsController.dataCacheConnector must be(DataCacheConnector)
    }

    "unauthorised users" must {
      "respond with a redirect" in {
        businessDetailsWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        businessDetailsWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }

    "return Review Details view" in {
      businessDetailsWithAuthorisedUser { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Welcome to ATED subscription")

        document.getElementById("business-name").text must be("ACME")
        document.getElementById("business-type").text must be("Limited")
        document.getElementById("business-address").text must be("Address")

        document.select(".button").text must be("Continue")
        document.select(".cancel-subscription-button").text must be("Cancel Subscription")
        document.select(".nested-banner").text must be("You are now ready to subscribe to ATED with the following business details. You can update your details on the following pages.")
      }
    }

    "read existing business details data from cache (without updating data)" in {
      val testDetailsController = businessDetailsWithAuthorisedUser { result =>
        status(result) must be(OK)
      }
      testDetailsController.dataCacheConnector.reads must be(1)
    }

  }


  "redirect-to-service " must {

    "unauthorised users" must {
      "respond with a redirect" in {
        redirectToServiceWithUnAuthorisedUser(service) { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        redirectToServiceWithUnAuthorisedUser(service) { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }

    "Authorised Users" must {

      "return service start page" in {

        redirectToServiceWithAuthorisedUser(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/account-summary")
        }
      }

      "throw an exception if it's an unknown service" in {
        redirectToServiceWithAuthorisedUser("unknownServiceTest") {
          result =>
            val thrown = the[RuntimeException] thrownBy redirectLocation(result).get
            thrown.getMessage must include("Service does not exist for : unknownServiceTest")
        }
      }
    }
  }

  private def setAuthorisedUser(userId : String) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }
  }

  private def setUnAuthorisedUser(userId : String) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }
  }

  private def fakeRequestWithSession(userId : String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  private def redirectToServiceWithUnAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    setUnAuthorisedUser(userId)
    val result = testReviewDetailsController.redirectToService(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def redirectToServiceWithAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    setAuthorisedUser(userId)
    val result = testReviewDetailsController.redirectToService(service).apply(fakeRequestWithSession(userId))
    test(result)
  }


  def businessDetailsWithAuthorisedUser(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    setAuthorisedUser(userId)
    val testDetailsController = testReviewDetailsController
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    setUnAuthorisedUser(userId)
    val result = testReviewDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
  }
}
