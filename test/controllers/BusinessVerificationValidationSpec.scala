package controllers

import java.util.UUID

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
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"
  val matchUtr = new SaUtrGenerator().nextSaUtr
  val noMatchUtr = new SaUtrGenerator().nextSaUtr

  val matchSuccessResponseUIB = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Unincorporated body",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

  val matchSuccessResponseLTD = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited company",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

  val matchSuccessResponseSOP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Sole trader",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

  val matchSuccessResponseOBP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Ordinary business partnership",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

  val matchSuccessResponseLLP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited liability partnership",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

  val matchSuccessResponseLP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited partnership",
      |  "businessAddress": {
      |    "line_1": "23 High Street",
      |    "line_2": "Park View",
      |    "line_3": "Gloucester",
      |    "line_4": "Gloucestershire",
      |    "postcode": "NE98 1ZZ",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123"
      |}
    """.stripMargin)

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

    type InputRequest = FakeRequest[AnyContentAsFormUrlEncoded]
    type MustTestMessage = String
    type InTestMessage = String
    type ErrorMessage = String
    type BusinessType = String

    def ctUtrRequest(ct: String = matchUtr.utr, businessName: String = "ACME") = request.withFormUrlEncodedBody("cotaxUTR" -> s"$ct", "businessName" -> s"$businessName")
    def psaUtrRequest(psa: String = matchUtr.utr, businessName: String = "ACME") = request.withFormUrlEncodedBody("psaUTR" -> s"$psa", "businessName" -> s"$businessName")
    def saUtrRequest(sa: String = matchUtr.utr, firstName: String = "A", lastName: String = "B") = request.withFormUrlEncodedBody("saUTR" -> s"$sa", "firstName" -> s"$firstName", "lastName" -> s"$lastName")

    val formValidationInputDataSetOrg: Seq[(MustTestMessage, Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)])] =
      Seq(
        ("if the selection is Unincorporated body :",
          Seq(
            ("Business Name must not be empty", "UIB", ctUtrRequest(businessName = ""), "Registered company name must be entered"),
            ("CO Tax UTR must not be empty", "UIB", ctUtrRequest(ct = ""), "Corporation Tax Unique Tax Reference must be entered"),
            ("Registered Name must not be more than 105 characters", "UIB", ctUtrRequest(businessName = "a" * 106), "Registered company name must not be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "UIB", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Tax Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "UIB", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Tax Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "UIB", ctUtrRequest(ct = "1234567890"), "Corporation Tax Unique Tax Reference is not valid")
          )
          ),
        ("if the selection is Limited Company :",
          Seq(
            ("Business Name must not be empty", "LTD", ctUtrRequest(businessName = ""), "Registered company name must be entered"),
            ("CO Tax UTR must not be empty", "LTD", ctUtrRequest(ct = ""), "Corporation Tax Unique Tax Reference must be entered"),
            ("Registered Name must not be more than 105 characters", "LTD", ctUtrRequest(businessName = "a" * 106), "Registered company name must not be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "LTD", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Tax Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "LTD", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Tax Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "LTD", ctUtrRequest(ct = "1234567890"), "Corporation Tax Unique Tax Reference is not valid")
          )
          ),
        ("if the selection is Limited Liability Partnership : ",
          Seq(
            ("Business Name must not be empty", "LLP", psaUtrRequest(businessName = ""), "Registered company name must be entered"),
            ("Partnership Self Assessment UTR  must not be empty", "LLP", psaUtrRequest(psa = ""), "Partnership Self Assessment Unique Tax Reference must be entered"),
            ("Registered Name must not be more than 105 characters", "LLP", psaUtrRequest(businessName = "a" * 106), "Registered company name must not be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LLP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LLP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LLP", psaUtrRequest(psa = "1234567890"), "Partnership Self Assessment Unique Tax Reference is not valid")
          )
          ),
        ("if the selection is Limited Partnership : ",
          Seq(
            ("Business Name must not be empty", "LP", psaUtrRequest(businessName = ""), "Registered company name must be entered"),
            ("Partnership Self Assessment UTR  must not be empty", "LP", psaUtrRequest(psa = ""), "Partnership Self Assessment Unique Tax Reference must be entered"),
            ("Registered Name must not be more than 105 characters", "LP", psaUtrRequest(businessName = "a" * 106), "Registered company name must not be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LP", psaUtrRequest(psa = "1234567890"), "Partnership Self Assessment Unique Tax Reference is not valid")
          )
          ),
        ("if the selection is Ordinary Business Partnership : ",
          Seq(
            ("Business Name must not be empty", "OBP", psaUtrRequest(businessName = ""), "Registered company name must be entered"),
            ("Partnership Self Assessment UTR  must not be empty", "OBP", psaUtrRequest(psa = ""), "Partnership Self Assessment Unique Tax Reference must be entered"),
            ("Registered Name must not be more than 105 characters", "OBP", psaUtrRequest(businessName = "a" * 106), "Registered company name must not be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "OBP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "OBP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Tax Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "OBP", psaUtrRequest(psa = "1234567890"), "Partnership Self Assessment Unique Tax Reference is not valid")
          )
          )
      )

    formValidationInputDataSetOrg foreach { dataSet =>
      s"${dataSet._1}" must {
        dataSet._2 foreach { inputData =>
          s"${inputData._1}" in {
            submitWithAuthorisedUserSuccessOrg(inputData._2, inputData._3) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(s"${inputData._4}")
            }
          }
        }
      }
    }

    val formValidationInputDataSetInd: Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)] =
      Seq(
        ("First name must not be empty", "SOP", saUtrRequest(matchUtr.utr, "", "b"), "First name must be entered"),
        ("Last name must not be empty", "SOP", saUtrRequest(lastName = ""), "Last name must be entered"),
        ("SA UTR must not be empty", "SOP", saUtrRequest(sa = ""), "Self Assessment Unique Tax Reference must be entered"),
        ("First Name must not be more than 40 characters", "SOP", saUtrRequest(firstName = "a" * 41), "First name must not be more than 40 characters"),
        ("Last Name must not be more than 40 characters", "SOP", saUtrRequest(lastName = "a" * 41), "Last name must not be more than 40 characters"),
        ("SA UTR must be 10 digits", "SOP", saUtrRequest(sa = "12345678901"), "Self Assessment Unique Tax Reference must be 10 digits"),
        ("SA UTR must contain only digits", "SOP", saUtrRequest(sa = "12345678aa"), "Self Assessment Unique Tax Reference must be 10 digits"),
        ("SA UTR must be valid", "SOP", saUtrRequest(sa = "1234567890"), "Self Assessment Unique Tax Reference is not valid")
      )

    "if the selection is Sole Trader:" must {

      formValidationInputDataSetInd foreach { dataSet =>
        s"${dataSet._1}" in {
          submitWithAuthorisedUserSuccessIndividual(dataSet._2, dataSet._3) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include(s"${dataSet._4}")
          }
        }
      }

    }


    "if the Ordinary Business Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-obp-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
        }
      }
    }

    "if the Limited Liability Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-llp-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
        }
      }
    }

    "if the Limited Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-lp-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
        }
      }
    }

    "if the Sole Trader form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailureIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-sop-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
        }
      }
    }

    "if the Unincorporated body form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-uib-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
        }
      }
    }

    "if the Limited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be BadRequest and  user should be on same page with validation error" in {
        submitWithAuthorisedUserFailure("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Smith & Co")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-type-ltd-form-error").text() must be("Your business details have not been found. Check that the details you have entered match the details held by Companies House and try again")
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
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))

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

    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
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
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
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
    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
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

