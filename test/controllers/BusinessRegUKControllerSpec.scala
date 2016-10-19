package controllers

import java.util.UUID

import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future


class BusinessRegUKControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestBusinessRegController extends BusinessRegUKController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  val serviceName: String = "ATED"

  "BusinessGBController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register-gb/$serviceName/GROUP")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }


      "respond with a redirect for /send & be redirected to the unauthorised page" in {
        submitWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }

    }

    "Authorised Users" must {

      "return business registration view for a user for Group" in {

        registerWithAuthorisedUser("awrs", "GROUP") {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Create AWRS group")
            document.getElementById("business-verification-text").text() must be("AWRS account registration")
            document.getElementById("business-registration.header").text() must be("Create AWRS group")

            document.getElementById("businessName_field").text() must be("Group representative name This is your registered company name")
            document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
            document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
            document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (Optional)")
            document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (Optional)")
            document.getElementById("submit").text() must be("Continue")
            document.getElementById("businessAddress.postcode_field").text() must be("Postcode")
            document.getElementById("businessAddress.country").attr("value") must be("GB")
        }
      }

      "return business registration view for a user for New Business" in {

        registerWithAuthorisedUser("awrs", "NEW") {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Create AWRS group")
            document.getElementById("business-verification-text").text() must be("AWRS account registration")
            document.getElementById("business-registration.header").text() must be("New business details")
            document.getElementById("businessName_field").text() must be("Group representative name This is your registered company name")
            document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
            document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
            document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (Optional)")
            document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (Optional)")
            document.getElementById("submit").text() must be("Continue")
        }
      }
    }

    "send" must {

      "validate form" must {

        def createJson(businessName: String = "ACME",
                       line1: String = "line-1",
                       line2: String = "line-2",
                       line3: String = "",
                       line4: String = "",
                       postcode: String = "NE981ZZ",
                       country: String = "GB",
                       hasBusinessUniqueId: Boolean = false,
                       bUId: String = "",
                       issuingInstitution: String = "",
                       issuingCountry: String = "") =
          Json.parse(
            s"""
               |{
               |  "businessName": "$businessName",
               |  "businessAddress": {
               |    "line_1": "$line1",
               |    "line_2": "$line2",
               |    "line_3": "$line3",
               |    "line_4": "$line4",
               |    "postcode": "$postcode",
               |    "country": "$country"
               |  },
               |  "hasBusinessUniqueId": $hasBusinessUniqueId,
               |  "businessUniqueId": "$bUId",
               |  "issuingInstitution": "$issuingInstitution",
               |  "issuingCountry": "$issuingCountry"
               |}
          """.stripMargin)

        type InputJson = JsValue
        type TestMessage = String
        type ErrorMessage = String

        "not be empty for a Group" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", postcode = "")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("You must enter a business name")
              contentAsString(result) must include("You must enter an address into Address line 1.")
              contentAsString(result) must include("You must enter an address into Address line 2.")
              contentAsString(result) must include("You must enter a postcode.")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(businessName = "a" * 106), "If entered, Business name must be maximum of 105 characters", "The business name cannot be more than 105 characters."),
          (createJson(line1 = "a" * 36), "If entered, Address line 1 must be maximum of 35 characters", "Address line 1 cannot be more than 35 characters."),
          (createJson(line2 = "a" * 36), "If entered, Address line 2 must be maximum of 35 characters", "Address line 2 cannot be more than 35 characters."),
          (createJson(line3 = "a" * 36), "Address line 3 is optional but if entered, must be maximum of 35 characters", "Address line 3 cannot be more than 35 characters."),
          (createJson(line4 = "a" * 36), "Address line 4 is optional but if entered, must be maximum of 35 characters", "Address line 4 cannot be more than 35 characters."),
          (createJson(postcode = "a" * 11), "If entered, Postcode must be maximum of 10 characters", "The postcode cannot be more than 10 characters."),
          (createJson(postcode = "1234567890"), "If entered, Postcode must be a valid postcode", "The postcode is invalid")
        )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1)) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If registration details entered are valid, continue button must redirect to review details page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            println(s"document = ${contentAsString(result)}")
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
          }
        }
      }
    }
  }

  def registerWithUnAuthorisedUser(businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestBusinessRegController.register(serviceName, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(service: String, businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.register(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


  def submitWithUnAuthorisedUser(businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.send(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.registerBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(successModel))

    val result = TestBusinessRegController.send(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


}

