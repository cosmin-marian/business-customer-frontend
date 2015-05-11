package controllers

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Org, Nino}
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{OrgAccount, PayeAccount, Accounts, Authority}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]

  object TestBusinessRegController extends BusinessRegController {
    override val authConnector = mockAuthConnector
  }

  val serviceName: String = "ATED"

  "BusinessRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName")).get
      status(result) must not be (NOT_FOUND)
    }

    "unauthorised users" must {
      "respond with a redirect" in {
        registerWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser { result =>
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
            document.getElementById("business-registration.header").text() must be("Add business details")
            document.getElementById("businessName_field").text() must be("Business name")
            document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
            document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
            document.getElementById("businessAddress.line_3_field").text() must be("Address line 3")
            document.getElementById("businessAddress.line_4_field").text() must be("Address line 4")
            document.getElementById("businessAddress.country_field").text() must be("Country")
            document.getElementById("submit").text() must be("Save and continue")
            document.getElementById("cancel").text() must be("Cancel")
        }
      }

      "send" must {

        "validate form" must {

          "not be empty" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)


            contentAsString(result) must include("Business name must be entered")
            contentAsString(result) must include("Address Line 1 must be entered")
            contentAsString(result) must include("Address Line 2 must be entered")
            contentAsString(result) must include("Country must be entered")
          }


          "If entered, Business name must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Business name must not be more than 40 characters")
          }

          "If entered, Address line 1 must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_2": "", "line_3": "", "line_4": "", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Address must not be more than 40 characters")
          }

          "If entered, Address line 2 must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_3": "", "line_4": "", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Address must not be more than 40 characters")
          }


          "Address line 3 is optional but if entered, must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "line_4": "", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Address must not be more than 40 characters")
          }

          "Address line 4 is optional but if entered, must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1", "country": ""} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Address must not be more than 40 characters")
          }

          "Country must be maximum of 40 characters" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "", "businessAddress": {"line_1": "", "line_2": "", "line_3": "", "line_4": "", "country": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"} }""")))
            status(result) must be(BAD_REQUEST)

            contentAsString(result) must include("Country must not be more than 40 characters")
          }

          "If registration details entered are valid, save and continue button must redirect to review details page" in {

            val result = TestBusinessRegController.send(service).apply(request.withJsonBody(Json.parse( """{ "businessName": "ddd", "businessAddress": {"line_1": "ddd", "line_2": "ddd", "line_3": "", "line_4": "", "country": "England"} }""")))
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/review-details/ATED")

          }
        }
      }
    }
  }



  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }

    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestBusinessRegController.register(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
