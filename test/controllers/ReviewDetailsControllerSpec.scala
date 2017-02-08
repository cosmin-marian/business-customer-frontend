package controllers

import java.util.UUID

import config.BusinessCustomerSessionCache
import connectors.DataCacheConnector
import models.{Address, EnrolResponse, Identifier, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRegistrationService
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]

  val mockAgentRegistrationService = mock[AgentRegistrationService]

  val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "GB")

  val directMatchReviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = true, Some("agent123"))

  val nonDirectMatchReviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

  def testReviewDetailsController(reviewDetails: ReviewDetails) = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = BusinessCustomerSessionCache
      override val sourceId: String = "Test"

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(reviewDetails))
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
      override val sessionCache = BusinessCustomerSessionCache
      override val sourceId: String = "Test"

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

  override def beforeEach = {
    reset(mockAgentRegistrationService)
    reset(mockAuthConnector)
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
        thrown.getMessage must include("We could not find your details. Check and try again.")

      }
    }

    "return Review Details view for a user, when user can't be directly found with login credentials" in {
      businessDetailsWithAuthorisedUser(nonDirectMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your business details")
      }
    }

    "return Review Details view for a user, when we directly found this user" in {
      businessDetailsWithAuthorisedUser(directMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your business details")
      }
    }

    "return Review Details view for an agent, when agent can't be directly found with login credentials" in {
      businessDetailsWithAuthorisedAgent(nonDirectMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your agency details")
      }
    }

    "return Review Details view for an agent, when we directly found this agent" in {
      businessDetailsWithAuthorisedAgent(directMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your agency details")
      }
    }

    "return Review Details view for an agent, when we directly found this agent with editable address" in {
      businessDetailsWithAuthorisedAgent(directMatchReviewDetails.copy(isBusinessDetailsEditable = true)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Check your agency details")
      }
    }

    "read existing business details data from cache (without updating data)" in {
      val testDetailsController = businessDetailsWithAuthorisedUser(nonDirectMatchReviewDetails) { result =>
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

      "throw an exception if different agent try to register with same details" in {

        continueWithUnAuthorisedAgent(service) {
          result =>
            val thrown = the[RuntimeException] thrownBy redirectLocation(result).get
            thrown.getMessage must include("William Words Worth to decide...........")
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


  private def fakeRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  private def continueWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithUnAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY)))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  def businessDetailsWithAuthorisedAgent(reviewDetails: ReviewDetails)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsController(reviewDetails)
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithAuthorisedUser(reviewDetails: ReviewDetails)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsController(reviewDetails)
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithAuthorisedUserNotFound(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val testDetailsController = testReviewDetailsControllerNotFound
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
  }

}
