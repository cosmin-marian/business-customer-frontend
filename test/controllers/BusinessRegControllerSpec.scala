package controllers

import java.util.UUID

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.domain.{Nino, Org}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestBusinessRegController extends BusinessRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  val serviceName: String = "ATED"

  "BusinessRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName")).get
      status(result) must not be (NOT_FOUND)
    }

    "unauthorised users" must {
      "respond with a redirect for /register" in {
        registerWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page for /register" in {
        registerWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }

      "respond with a redirect for /send" in {
        submitWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page for /send" in {
        submitWithUnAuthorisedUser { result =>
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
            document.getElementById("business-verification-text").text() must be("ATED account registration")
            document.getElementById("business-registration.header").text() must be("Non-UK business details")
            document.getElementById("business-registration-subheader").text() must be("You need to tell us the details of your business.")
            document.getElementById("businessName_field").text() must be("Business name")
            document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
            document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
            document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
            document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
            document.getElementById("businessAddress.country_field").text() must be("Country")
            document.getElementById("businessUniqueId_field").text() must be("Business Unique Id (optional)")
            document.getElementById("issuingInstitution_field").text() must be("Institution that issued the Business Unique Identifier (optional)")
            document.getElementById("submit").text() must be("Continue")
            document.getElementById("back").text() must be("Back")
        }
      }
    }

    "send" must {

      "validate form" must {

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""}, "businessUniqueId": "", "issuingInstitution": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Business name must be entered")
              contentAsString(result) must include("Address Line 1 must be entered")
              contentAsString(result) must include("Address Line 2 must be entered")
              contentAsString(result) must include("Country must be entered")
          }
        }


        "If entered, Business name must be maximum of 40 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""}, "businessUniqueId": "", "issuingInstitution": "" }""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Business name must not be more than 40 characters")
          }
        }

        "If entered, Address line 1 must be maximum of 40 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_2": "", "line_3": "", "line_4": "", "country": ""}, "businessUniqueId": "", "issuingInstitution": "" }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Address line 1 must not be more than 35 characters")
          }
        }

        "If entered, Address line 2 must be maximum of 40 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_3": "", "line_4": "", "country": ""} }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Address line 2 must not be more than 35 characters")
          }
        }


        "Address line 3 is optional but if entered, must be maximum of 40 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_4": "", "country": ""} }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Address line 3 must not be more than 35 characters")
          }
        }

        "Address line 4 is optional but if entered, must be maximum of 40 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "country": ""} }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Address line 4 must not be more than 35 characters")
          }
        }

        "Country must be maximum of 2 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"} }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Country must not be more than 2 characters")
          }
        }

        "businessUniqueId must be maximum of 60 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""}, "businessUniqueId": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD111111111112222222222", "issuingInstitution": "" }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Business Unique Id must not be more than 60 characters")
          }
        }

        "issuingInstitution must be maximum of 60 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""}, "businessUniqueId": "", "issuingInstitution": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1" }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Institution that issued the Business Unique Identifier must not be more than 40 characters")
          }
        }

        "If registration details entered are valid, continue button must redirect to review details page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "businessName": "ddd", "businessAddress": {"line_1": "ddd", "line_2": "ddd", "line_3": "", "line_4": "", "country": "UK"}, "businessUniqueId": "id1", "issuingInstitution": "institutionName" }""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/review-details/$service")
          }
        }
      }

      "Back" must {

        "respond with redirect" in {
          backWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the business verification page" in {

          backWithAuthorisedUser {
            result =>
              redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
          }

        }
      }
    }
  }

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def backWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.back(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.send(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"),Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", "Unincorporated body", address)

    when(mockBusinessRegistrationService.registerNonUk(Matchers.any())(Matchers.any())).thenReturn(Future.successful(successModel))

    val result = TestBusinessRegController.send(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
