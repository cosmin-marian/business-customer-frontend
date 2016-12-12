package controllers.nonUKReg

import java.util.UUID

import models.{BusinessRegistration, Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
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


class UpdateNonUKBusinessRegistrationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestNonUKController extends UpdateNonUKBusinessRegistrationController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessRegistrationService)
  }

  val serviceName: String = "ATED"

  "UpdateNonUKBusinessRegistrationController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName/NUK")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        editWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }

      "respond with a redirect for /send & be redirected to the unauthorised page" in {
        submitWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "edit client" must {

      "return business registration view for a Non-UK based client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData))))

        editClientWithAuthorisedUser(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-reg-header").text() must be("Enter your overseas business details")
          document.getElementById("business-reg-lede").text() must be("This is the registered address of your overseas business.")

          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("hasBusinessUniqueId").text() must include("Do they have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "return business registration view for a Non-UK based agent creating a client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData))))

        editClientWithAuthorisedAgent(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Enter your client's overseas business details")
          document.getElementById("business-verification-text").text() must be("Add a client")
          document.getElementById("business-reg-header").text() must be("Enter your client's overseas business details")
          document.getElementById("business-reg-lede").text() must be("This is the registered address of your client's overseas business.")

          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("hasBusinessUniqueId").text() must include("Do they have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "throw an exception if we have no data" in {

        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        editClientWithAuthorisedUser(serviceName, "NUK") { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("No Registration Details found")
        }
      }
    }

    "edit agent" must {

      "return business registration view for a Non-UK based client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData))))

        editAgentWithAuthorisedUser(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("business-verification-text").text() must be("ATED registration")
          document.getElementById("business-reg-header").text() must be("Enter your overseas business details")
          document.getElementById("business-reg-lede").text() must be("This is the registered address of your overseas business.")

          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("hasBusinessUniqueId").text() must include("Do they have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "return business registration view for a Non-UK based agent creating a client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData))))

        editAgentWithAuthorisedAgent(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Enter your overseas agent details")
          document.getElementById("business-reg-header").text() must be("Enter your overseas agent details")

          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("hasBusinessUniqueId").text() must include("Do they have an overseas company registration number?")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "throw an exception if we have no data" in {

        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        editAgentWithAuthorisedUser(serviceName, "NUK") { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("No Registration Details found")
        }
      }
    }

    "update" must {

      "validate form" must {

        def createJson(businessName: String = "ACME",
                       line1: String = "line-1",
                       line2: String = "line-2",
                       line3: String = "",
                       line4: String = "",
                       country: String = "FR",
                       hasBusinessUniqueId: Boolean = true,
                       bUId: String = "some-id",
                       issuingInstitution: String = "some-institution",
                       issuingCountry: String = "FR") =
          Json.parse(
            s"""
               |{
               |  "businessName": "$businessName",
               |  "businessAddress": {
               |    "line_1": "$line1",
               |    "line_2": "$line2",
               |    "line_3": "$line3",
               |    "line_4": "$line4",
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

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", country = "", bUId = "", issuingInstitution = "", issuingCountry = "")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("You must enter a business name")
            contentAsString(result) must include("You must enter an address into Address line 1.")
            contentAsString(result) must include("You must enter an address into Address line 2.")
            contentAsString(result) mustNot include("Postcode must be entered")
            contentAsString(result) must include("You must enter a country")
            contentAsString(result) must include("You must enter a country that issued the business unique identifier.")
            contentAsString(result) must include("You must enter an institution that issued the business unique identifier.")
            contentAsString(result) must include("You must enter a Business Unique Identifier.")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(businessName = "a" * 106), "If entered, Business name must be maximum of 105 characters", "The business name cannot be more than 105 characters."),
          (createJson(line1 = "a" * 36), "If entered, Address line 1 must be maximum of 35 characters", "Address line 1 cannot be more than 35 characters."),
          (createJson(line2 = "a" * 36), "If entered, Address line 2 must be maximum of 35 characters", "Address line 2 cannot be more than 35 characters."),
          (createJson(line3 = "a" * 36), "Address line 3 is optional but if entered, must be maximum of 35 characters", "Address line 3 cannot be more than 35 characters."),
          (createJson(line4 = "a" * 36), "Address line 4 is optional but if entered, must be maximum of 35 characters", "Address line 4 cannot be more than 35 characters."),
          (createJson(country = "GB"), "show an error if country is selected as GB", "You cannot select United Kingdom when entering an overseas address"),
          (createJson(bUId = "a" * 61), "businessUniqueId must be maximum of 60 characters", "Business Unique Identifier cannot be more than 60 characters."),
          (createJson(issuingInstitution = "a" * 41), "issuingInstitution must be maximum of 40 characters", "The institution that issued the Business Unique Identifier cannot be more than 40 characters."),
          (createJson(issuingCountry = "GB"), "show an error if issuing country is selected as GB", "You cannot select United Kingdom when entering an overseas address")
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

        "If registration details entered are valid, continue button must redirect to service specific redirect url" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some("http://localhost:9933/ated-subscription/registered-business-address")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9933/ated-subscription/registered-business-address"))
          }
        }

        "valid registration details are entered and BUId question is selected as No, continue button must redirect to service specific redirect url" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(hasBusinessUniqueId = false, issuingCountry = "", issuingInstitution = "", bUId = "")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some("http://localhost:9933/ated-subscription/registered-business-address")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9933/ated-subscription/registered-business-address"))
          }
        }

        "redirect to the review details page if we have no redirect url" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(hasBusinessUniqueId = false, issuingCountry = "", issuingInstitution = "", bUId = "")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", None) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/business-customer/review-details/ATED"))
          }
        }

      }
    }
  }

  def editWithUnAuthorisedUser(businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestNonUKController.edit(serviceName, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedAgent(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestNonUKController.edit(serviceName, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedUser(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"


    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))


    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.edit(serviceName, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editAgentWithAuthorisedAgent(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestNonUKController.editAgent(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editAgentWithAuthorisedUser(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"


    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))


    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.editAgent(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(businessType: String = "NUK", redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, redirectUrl, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], service: String = service, redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))

    val result = TestNonUKController.update(service, redirectUrl, true).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, redirectUrl, true).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}