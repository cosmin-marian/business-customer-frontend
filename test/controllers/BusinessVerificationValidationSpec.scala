package controllers

import java.util.UUID

import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"

  val matchSuccessResponseUIB = Json.parse( """{ "businessName":"ACME", "businessType":"Unincorporated body", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchSuccessResponseLTD = Json.parse( """{ "businessName":"ACME", "businessType":"Limited company", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchSuccessResponseSOP = Json.parse( """{ "businessName":"ACME", "businessType":"Sole trader", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchSuccessResponseOBP = Json.parse( """{ "businessName":"ACME", "businessType":"Ordinary business partnership", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchSuccessResponseLLP = Json.parse( """{ "businessName":"ACME", "businessType":"Limited liability partnership", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchSuccessResponseLP = Json.parse( """{ "businessName":"ACME", "businessType":"Limited partnership", "businessAddress": {"line_1": "23 High Street", "line_2": "Park View", "line_3": "Gloucester", "line_4": "Gloucestershire", "postcode": "NE98 1ZZ", "country": "UK"}, "sapNumber": "sap123", "safeId": "safe123", "agentReferenceNumber": "agent123" }""")
  val matchFailureResponse = Json.parse( """{"reason":"Sorry. Business details not found. Try with correct UTR and/or name."}""")

  def submitWithUnAuthorisedUser(businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.submit(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  "BusinessVerificationController" must {

    "if the selection is Unincorporated body :" must {
      "Business Name must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Registered name must be entered")

            document.getElementById("businessName_field").text() must include("Registered name")
            document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Tax Reference (UTR)")
        }
      }

      "CO Tax UTR must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
        }
      }


      "Registered Name must not be more than 105 characters" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("businessName" -> "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa12", "cotaxUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Registered name must not be more than 105 characters")
        }
      }

      "CO Tax UTR must be 10 digits" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
        }
      }

      "CO Tax UTR must contain only letters" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
        }
      }


      "CO Tax UTR must be valid" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
        }
      }
    }


    "if the selection is Limited Company :" must {
      "Business Name must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            contentAsString(result) must include("Registered name must be entered")
            document.getElementById("businessName_field").text() must include("Registered name")
            document.getElementById("cotaxUTR_field").text() must include("Corporation Tax Unique Tax Reference (UTR)")
        }
      }

      "CO Tax UTR must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
        }
      }


      "Register Name must not be more than 105 characters" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("businessName" -> "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa12", "cotaxUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Registered name must not be more than 105 characters")
        }
      }

      "CO Tax UTR must be 10 digits" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
        }
      }

      "CO Tax UTR must be valid" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
        }
      }

      "CO Tax UTR must contain only letters" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
        }
      }
    }


    "if the selection is Sole Trader:" must {
      "First name, last name and SA UTR  must be entered" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "", "lastName" -> "", "saUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("First Name must be entered")
            contentAsString(result) must include("Last Name must be entered")
            contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")


            document.getElementById("firstName_field").text() must include("First Name")
            document.getElementById("lastName_field").text() must include("Last Name")
            document.getElementById("saUTR_field").text() must include("Self Assessment Unique Tax Reference")
        }
      }

      "SA UTR must be valid" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "saUTR" -> "1234567892")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
        }
      }

      "First Name and Last Name must not be more than 40 characters" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "lastName" -> "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("First Name must not be more than 40 characters")
            contentAsString(result) must include("Last Name must not be more than 40 characters")
        }
      }

      "SA UTR must be 10 digits" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "Smith & Co", "lastName" -> "Mohombi", "saUTR" -> "11111111111")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
        }
      }


      "SA UTR must contain only letters" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "Smith & Co", "lastName" -> "Mohombi", "saUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
        }
      }

    }

    "if the selection is Limited Liability Partnership:" must {
      "Business Name and CO Tax UTR must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Registered name must be entered")
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
            document.getElementById("businessName_field").text() must include("Registered name")
            document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
        }
      }

      "Registered name must not be more than 105 characters" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa12", "psaUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Registered name must not be more than 105 characters")
        }
      }

      "Partnership Self Assessment UTR must be 10 digits" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
        }
      }

      "Partnership Self Assessment UTR must be valid" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
        }
      }

      "Partnership Self Assessment UTR must contain only letters" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
        }
      }

    }

    "if the selection is Ordinary Business Partnership :" must {
      "Business Name and CO Tax UTR must not be empty" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include("Registered name must be entered")
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
            document.getElementById("businessName_field").text() must include("Registered name")
            document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
        }
      }

      "Registered Name must not be more than 105 characters" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa12", "psaUTR" -> "")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Registered name must not be more than 105 characters")
        }
      }

      "Partnership Self Assessment UTR must be 10 digits" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
        }
      }

      "Partnership Self Assessment UTR must be valid" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
        }
      }

      "Partnership Self Assessment must contain only letters" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
        }
      }
    }

    "if the Ordinary Business Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111112")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-obp-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "if the Limited Liability Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111112")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-llp-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "if the Limited Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111112")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-lp-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "if the Sole Trader form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> "1111111111")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailureIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> "1111111112")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-sop-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "if the Unincorporated body form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111112", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-uib-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "if the Limited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111112", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("#business-type-ltd-form").text() must be("Your business details have not been found. Please check that your details are correct and up-to-date and try again")
        }
      }
    }

    "submit" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          submitWithUnAuthorisedUser("") { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          submitWithUnAuthorisedUser("") { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }
    }

  }

  def submitWithAuthorisedUserSuccessOrg(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val matchSuccessResponse = businessType match {
      case "UIB" => matchSuccessResponseUIB
      case "LLP" => matchSuccessResponseLLP
      case "OBP" => matchSuccessResponseOBP
      case "LTD" => matchSuccessResponseLTD
      case "LP" => matchSuccessResponseLP
    }
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchSuccessResponse))
    val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire"), Some("NE98 1ZZ"), "U.K.")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", Some("agent123"))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccessIndividual(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchSuccessResponseSOP))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailureIndividual(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  object TestBusinessVerificationController extends BusinessVerificationController {
    override val businessMatchingService = mockBusinessMatchingService
    val authConnector = mockAuthConnector
  }

}

