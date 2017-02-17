package controllers.nonUKReg

import java.util.UUID

import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import models.{ReviewDetails, BusinessRegistration, Address}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future


class OverseasCompanyRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]
  val mockBusinessRegistrationCache = mock[BusinessRegCacheConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  object TestController extends OverseasCompanyRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
    override val businessRegistrationCache = mockBusinessRegistrationCache
    override val controllerId = "test"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  val serviceName: String = "ATED"

  "OverseasCompanyRegController" must {

    "respond to /view" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/non-uk-client/overseas-company/$serviceName/true")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /view & be redirected to the unauthorised page" in {
        viewWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "view" must {

      "return business registration view for a user for Non-UK" in {

        viewWithAuthorisedUser(serviceName) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number?")
        }
      }
    }

    "send" must {

      val regAddress = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
      val businessReg = BusinessRegistration("ACME", regAddress)
      val reviewDetails = ReviewDetails("ACME", Some("Unincorporated body"), regAddress, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

      "validate form" must {

        def createJson(hasBusinessUniqueId: Boolean = true,
                       bUId: String = "some-id",
                       issuingInstitution: String = "some-institution",
                       issuingCountry: String = "FR") =
          Json.parse(
            s"""
               |{
               |  "hasBusinessUniqueId": $hasBusinessUniqueId,
               |  "businessUniqueId": "$bUId",
               |  "issuingInstitution": "$issuingInstitution",
               |  "issuingCountry": "$issuingCountry"
               |}
          """.stripMargin)

        type InputJson = JsValue
        type TestMessage = String
        type ErrorMessage = String

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(bUId = "", issuingInstitution = "", issuingCountry = "")

          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg), reviewDetails) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("You must enter a country that issued the business unique identifier.")
            contentAsString(result) must include("You must enter an institution that issued the business unique identifier.")
            contentAsString(result) must include("You must enter a Business Unique Identifier.")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(bUId = "a" * 61), "businessUniqueId must be maximum of 60 characters", "Business Unique Identifier cannot be more than 60 characters."),
          (createJson(issuingInstitution = "a" * 41), "issuingInstitution must be maximum of 40 characters", "The institution that issued the Business Unique Identifier cannot be more than 40 characters."),
          (createJson(issuingCountry = "GB"), "show an error if issuing country is selected as GB", "You cannot select United Kingdom when entering an overseas address")
        )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1), "ATED", Some(businessReg), reviewDetails) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If we have no cache then an execption must be thrown" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", None, reviewDetails) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("[OverseasCompanyRegController][send] - service :ATED. Error : No Cached BusinessRegistration")
          }
        }

        "If registration details entered are valid, continue button must redirect to the redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg), reviewDetails) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/business-customer/review-details/ATED"))
          }
        }

        "If registration details entered are valid, continue button must redirect with to next page if no redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg), reviewDetails, Some("http://redirectHere")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://redirectHere"))
          }
        }
      }
    }
  }

  def viewWithUnAuthorisedUser()(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestController.view(serviceName, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def viewWithAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestController.view(service, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def viewWithAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestController.view(service, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson],
                                    service: String = service,
                                    busRegCache : Option[BusinessRegistration] = None,
                                    reviewDetails : ReviewDetails,
                                    redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBusinessRegistrationCache.fetchAndGetBusinessRegForSession(Matchers.any())).thenReturn(Future.successful(busRegCache))
    when(mockBusinessRegistrationService.registerBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(reviewDetails))

    val result = TestController.register(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestController.register(service, true).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
