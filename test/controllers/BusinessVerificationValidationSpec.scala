package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{Result, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, Org}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{PayeAccount, OrgAccount, Accounts, Authority}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"

  val matchSuccessResponseUIB = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
  val matchSuccessResponseLTD = Json.parse( """{"businessName":"ACME","businessType":"Limited company","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
  val matchSuccessResponseSOP = Json.parse( """{"businessName":"ACME","businessType":"Sole trader","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
  val matchSuccessResponseOBP = Json.parse( """{"businessName":"ACME","businessType":"Ordinary business partnership","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
  val matchSuccessResponseLLP = Json.parse( """{"businessName":"ACME","businessType":"Limited liability partnership","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")


  object TestBusinessVerificationController extends BusinessVerificationController {
    val dataCacheConnector = mockDataCacheConnector
    val authConnector = mockAuthConnector
    val businessMatchingConnector = mockBusinessMatchingConnector
  }

  "if the selection is Unincorporated body :" must {
    "Business Name must not be empty" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")

          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
      }
    }

    "CO Tax UTR must not be empty" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
      }
    }


    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "CO Tax UTR must be 10 digits" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }

    "CO Tax UTR must contain only letters" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "111111111a")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }


    "CO Tax UTR must be valid" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
      }
    }
  }


  "if the selection is Limited Company :" must {
    "Business Name must not be empty" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("Business Name must be entered")
          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
      }
    }

    "CO Tax UTR must not be empty" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
      }
    }


    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "CO Tax UTR must be 10 digits" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }

    "CO Tax UTR must be valid" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
      }
    }

    "CO Tax UTR must contain only letters" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "111111111a")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }
  }


  "if the selection is Sole Trader:" must {
    "First name, last name and SA UTR  must be entered" in {
      submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("firstName" -> "", "lastName" -> "", "saUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("First Name must be entered")
          contentAsString(result) must include("Last Name must be entered")
          contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")


          document.getElementById("first-name_field").text() must include("First Name")
          document.getElementById("last-name_field").text() must include("Last Name")
          document.getElementById("saUTR_field").text() must include("Self Assessment Unique Tax Reference")
      }
    }

    "SA UTR must be valid" in {
      submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "saUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
      }
    }

    "First Name and Last Name must not be more than 40 characters" in {
      submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("firstName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "lastName" -> "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("First Name must not be more than 40 characters")
          contentAsString(result) must include("Last Name must not be more than 40 characters")
      }
    }

    "SA UTR must be 10 digits" in {
      submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("firstName" -> "Smith & Co", "lastName" -> "Mohombi", "saUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
      }
    }


      "SA UTR must contain only letters" in {
        submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("firstName" -> "Smith & Co", "lastName" -> "Mohombi", "saUTR" -> "111111111a")) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
        }
      }

  }

  "if the selection is Limited Liability Partnership:" must {
    "Business Name and CO Tax UTR must not be empty" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
      }
    }

    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
      }
    }

    "Partnership Self Assessment UTR must be valid" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }

    "Partnership Self Assessment UTR must contain only letters" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "111111111a")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }

  }

  "if the selection is Ordinary Business Partnership :" must {
    "Business Name and CO Tax UTR must not be empty" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
      }
    }

    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
      }
    }

    "Partnership Self Assessment UTR must be valid" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }

    "Partnership Self Assessment must contain only letters" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "111111111a")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }
  }

  "if the Ordinary Business Partnership form is successfully validated:" must {
    "for successful match, status should be 303 and  user should be redirected to review details page" in {
      submitWithAuthorisedUserSuccess("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }
    "for unsuccessful match, status should be 303 and  user should be redirected to hello world page" in {
      submitWithAuthorisedUserFailure("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111112")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/hello")
      }
    }
  }

  "if the Limited Liability Partnership form  is successfully validated:" must {
    "for successful match, status should be 303 and  user should be redirected to review details page" in {
      submitWithAuthorisedUserSuccess("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }
    "for unsuccessful match, status should be 303 and  user should be redirected to hello world page" in {
      submitWithAuthorisedUserFailure("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111112")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/hello")
      }
    }
  }

  "if the Sole Trader form  is successfully validated:" must {
    "for successful match, status should be 303 and  user should be redirected to review details page" in {
      submitWithAuthorisedUserSuccess("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> "1111111111")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }
    "for unsuccessful match, status should be 303 and  user should be redirected to hello world page" in {
      submitWithAuthorisedUserFailure("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> "1111111112")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/hello")
      }
    }
  }

  "if the Unincorporated body form  is successfully validated:" must {
    "for successful match, status should be 303 and  user should be redirected to review details page" in {
      submitWithAuthorisedUserSuccess("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }
    "for unsuccessful match, status should be 303 and  user should be redirected to hello world page" in {
      submitWithAuthorisedUserFailure("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111112", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/hello")
      }
    }
  }

  "if the Limited Company form  is successfully validated:" must {
    "for successful match, status should be 303 and  user should be redirected to review details page" in {
      submitWithAuthorisedUserSuccess("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/review-details/$service")
      }
    }
    "for unsuccessful match, status should be 303 and  user should be redirected to hello world page" in {
      submitWithAuthorisedUserFailure("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111112", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/hello")
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

  def submitWithUnAuthorisedUser(businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }


    val result = TestBusinessVerificationController.submit(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }
    val matchSuccessResponse = businessType match {
      case "UIB" => matchSuccessResponseUIB
      case "LLP" => matchSuccessResponseLLP
      case "OBP" => matchSuccessResponseOBP
      case "SOP" => matchSuccessResponseSOP
      case "LTD" => matchSuccessResponseLTD
    }

    val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> matchSuccessResponse))
    when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
    when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")
    val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> matchFailureResponse))
    when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.successful(matchFailureResponse))
    when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
