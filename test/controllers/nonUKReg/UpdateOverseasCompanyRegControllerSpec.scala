package controllers.nonUKReg

import java.util.UUID

import models.{OverseasCompany, BusinessRegistration, Address, ReviewDetails}
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


class UpdateOverseasCompanyRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestNonUKController extends UpdateOverseasCompanyRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessRegistrationService)
  }

  val serviceName: String = "ATED"

  "UpdateOverseasCompanyRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName/NUK")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        editWithUnAuthorisedUser() { result =>
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
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
        )
        val overseasCompany = OverseasCompany(
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))

        editClientWithAuthorisedUser(serviceName) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number?")
        }
      }

      "return business registration view for a Non-UK based agent creating a client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
        )
        val overseasCompany = OverseasCompany(
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))

        editClientWithAuthorisedAgent(serviceName) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number?")

        }
      }

      "throw an exception if we have no data" in {

        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        editClientWithAuthorisedUser(serviceName) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("No Registration Details found")
        }
      }
    }

    "update" must {

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

          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED") { result =>
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
            registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1), "ATED") { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If we have no cache then an exception must be thrown" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", None, false) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("No Registration Details found")
          }
        }

        "If registration details entered are valid, continue button must redirect to the redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED") { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/business-customer/review-details/ATED"))
          }
        }

        "If registration details entered are valid, continue button must redirect with to next page if no redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some("http://redirectHere")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://redirectHere"))
          }
        }
      }
    }
  }

  def editWithUnAuthorisedUser()(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestNonUKController.viewForUpdate(serviceName, false, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestNonUKController.viewForUpdate(serviceName, false, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"


    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))


    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.viewForUpdate(serviceName, false, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }



  def submitWithUnAuthorisedUser(businessType: String = "NUK", redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, true, redirectUrl).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], service: String = service, redirectUrl: Option[String] = None, hasCache: Boolean = true)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "U.K.")
    val busRegData = BusinessRegistration(businessName = "testName", businessAddress = address)
    val overseasCompany = OverseasCompany(
      businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
      hasBusinessUniqueId = Some(true),
      issuingInstitution = Some("issuingInstitution"),
      issuingCountry = None
    )
    if (hasCache)
      when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))
    else
      when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))

    val result = TestNonUKController.update(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
