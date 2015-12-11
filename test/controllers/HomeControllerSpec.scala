package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendAuthConnector
import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService = mock[BusinessMatchingService]

  val testAddress = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
  val testReviewDetails = ReviewDetails("ACME", Some("Limited"), testAddress, "sap123", "safe123", false, Some("agent123"))

  object TestHomeController extends HomeController {
    val businessMatchService: BusinessMatchingService = mockBusinessMatchingService
    override val authConnector = mockAuthConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBusinessMatchingService)
  }

  "HomeController" must {

    "implement correct Auth connector" in {
      HomeController.authConnector must be(FrontendAuthConnector)
    }

    "implement correct BusinessMatching service" in {
      HomeController.businessMatchService must be(BusinessMatchingService)
    }

    "homePage" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }

      "Unauthenticated users" must {
        "respond with a redirect" in {
          getWithUnAuthenticated { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthenticated { result =>
            redirectLocation(result).get must include("/account/sign-in")
          }
        }
      }

      "Authorised users must" must {
        "if have valid utr" must {
          "if match is found, be redirected to Review Details page" in {
            getWithAuthorisedUserMatched {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/review-details/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false))(Matchers.any(), Matchers.any())
            }
          }

          "if match is Not found, be redirected to Business verification page" in {
            getWithAuthorisedUserNotMatched {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false))(Matchers.any(), Matchers.any())
            }
          }
        }
        "if has no UTR, be redirected to Business verification page" in {
          getWithAuthorisedUserNoUTR {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
              verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false))(Matchers.any(), Matchers.any())
          }
        }
      }
    }

  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestHomeController.homePage(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestHomeController.homePage(service).apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedUserMatched(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val reviewDetails = Json.toJson(testReviewDetails)
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Some(Future.successful(reviewDetails)))
    val result = TestHomeController.homePage(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNotMatched(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val notFound = Json.parse( """{"Reason" : "Text from reason column"}""")
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Some(Future.successful(notFound)))
    val result = TestHomeController.homePage(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNoUTR(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(None)
    val result = TestHomeController.homePage(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
