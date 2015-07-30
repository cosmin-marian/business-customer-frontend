package controllers

import java.util.UUID

import connectors.DataCacheConnector
import models.{EnrolResponse, ReviewDetails, Address}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRegistrationService

import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockAgentRegistrationService = mock[AgentRegistrationService]
  val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"),Some("NE98 1ZZ"), "GB")
  def testReviewDetailsController = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", "agent123")))
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
      override val authConnector = mockAuthConnector
      override val agentRegistrationService = mockAgentRegistrationService
    }
  }

  def testReviewDetailsControllerNotFound = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = SessionCache

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(None)
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
      override val authConnector = mockAuthConnector
      override val agentRegistrationService = mockAgentRegistrationService
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
    "throw an exception if we have no review details" in {
      businessDetailsWithAuthorisedUserNotFound { result =>
        val thrown = the[RuntimeException] thrownBy contentAsString(result)
        thrown.getMessage must include("No Details were found")

      }
    }

    "return Review Details view for a user" in {
      businessDetailsWithAuthorisedUser { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Verify business details")
        document.getElementById("banner").text must be("You are about to register the following business for ATED.")
        document.getElementById("bc.business-registration.text").text() must be("ATED account registration")
        document.getElementById("business-name-label").text must be("Name")
        document.getElementById("business-address-label").text must be("Registered address")

        document.select(".button").text must be("Continue")
      }
    }

    "return Review Details view for an agent" in {

      businessDetailsWithAuthorisedAgent { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Verify agent details")
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


  "continue " must {

    "unauthorised users" must {
      "respond with a redirect" in {
        continueWithUnAuthorisedUser(service) { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        continueWithUnAuthorisedUser(service) { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }



    "Authorised Users" must {

      "return service start page correctly for ATED Users" in {

        continueWithAuthorisedUser(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/registered-business-address")
        }
      }

      "return service start page correctly for AWRS Users" in {

        continueWithAuthorisedUser("AWRS") {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/alcohol-wholesale-scheme")
        }
      }

      "return agent registration page correctly for Agents" in {

        continueWithAuthorisedAgent(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/agent/register")
        }
      }
      "throw an exception if it's an unknown service" in {
        continueWithAuthorisedUser("unknownServiceTest") {
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

  private def continueWithUnAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedUser(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedAgent(service : String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", friendlyName = "Main Enrolment",  identifiersForDisplay = "Ated_Ref_No")
    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any())).thenReturn(Future.successful(enrolSuccessResponse))
    val result = testReviewDetailsController.continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  def businessDetailsWithAuthorisedAgent(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsController
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
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

  def businessDetailsWithAuthorisedUserNotFound(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsControllerNotFound
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
