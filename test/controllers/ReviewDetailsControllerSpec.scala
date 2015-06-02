package controllers

import java.util.UUID

import connectors.DataCacheConnector
import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, Org}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), "U.K.")
  def testReviewDetailsController = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(ReviewDetails("ACME", "Limited", address)))
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
        document.select("h1").text must be("Verify business details")
        document.getElementById("bc.business-registration.text").text() must be("ATED account registration")
        document.getElementById("business-name").text must be("ACME")
        document.getElementById("business-type").text must be("Limited")
        document.getElementById("business-address").text must be(s"$address")

        document.select(".button").text must be("Continue")

        document.getElementById("banner").text must be("You are about to register the following business for ATED.")
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

      "return service start page correctly for ATED" in {

        redirectToServiceWithAuthorisedUser(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/registered-business-address")
        }
      }

      "return service start page correctly for AWRS" in {

        redirectToServiceWithAuthorisedUser("AWRS") {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/alcohol-wholesale-scheme")
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


  private def fakeRequestWithSession(userId : String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  private def redirectToServiceWithUnAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.redirectToService(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def redirectToServiceWithAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.redirectToService(service).apply(fakeRequestWithSession(userId))
    test(result)
  }


  def businessDetailsWithAuthorisedUser(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsController
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
  }
}
