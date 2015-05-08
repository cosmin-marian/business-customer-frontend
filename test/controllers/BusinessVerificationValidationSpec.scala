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

  object TestBusinessVerificationController extends BusinessVerificationController  {
    val dataCacheConnector = mockDataCacheConnector
    val authConnector = mockAuthConnector
    val businessMatchingConnector = mockBusinessMatchingConnector
  }

  "if the selection is Unincorporated body :" must {
    "Business Name must not be empty" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include("Business Name must be entered")

          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
      }
    }

    "CO Tax UTR must not be empty" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
      }
    }


    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "CO Tax UTR must be 10 digits" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }

    "CO Tax UTR must be valid" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
      }
    }
  }


  "if the selection is Limited Company :" must {
    "Business Name must not be empty" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("Business Name must be entered")
          document.getElementById("businessName_field").text() must include("Business Name")
          document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
      }
    }

    "CO Tax UTR must not be empty" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
      }
    }


    "Business Name must not be more than 40 characters" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "CO Tax UTR must be 10 digits" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
      }
    }

    "CO Tax UTR must be valid" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
      }
    }
  }


  "if the selection is Sole Trader:" must {
    "First name, last name and SA UTR  must be entered" in {
      submitWithAuthorisedUser("SOP", request.withFormUrlEncodedBody("firstName" -> "", "lastName" -> "", "saUTR" -> "")) {
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
      submitWithAuthorisedUser("SOP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "saUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
      }
    }

    "First Name and Last Name must not be more than 40 characters" in {
      submitWithAuthorisedUser("SOP", request.withFormUrlEncodedBody("firstName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "lastName" -> "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("First Name must not be more than 40 characters")
          contentAsString(result) must include("Last Name must not be more than 40 characters")
      }
    }

    "SA  UTR must be 10 digits" in {
      submitWithAuthorisedUser("SOP", request.withFormUrlEncodedBody("firstName" -> "Smith & Co", "lastName" -> "Mohombi", "saUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
      }
    }
  }

  "if the selection is Limited Liability Partnership:" must {
    "Business Name and CO Tax UTR must not be empty"  in {
      submitWithAuthorisedUser("LLP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
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
      submitWithAuthorisedUser("LLP", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      submitWithAuthorisedUser("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
      }
    }

    "Partnership Self Assessment UTR must be valid" in {
      submitWithAuthorisedUser("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }
  }

  "if the selection is Ordinary Business Partnership :" must {
    "Business Name and CO Tax UTR must not be empty"  in {
      submitWithAuthorisedUser("OBP", request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> "")) {
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
      submitWithAuthorisedUser("OBP", request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> "")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Business Name must not be more than 40 characters")
      }
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      submitWithAuthorisedUser("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
      }
    }

    "Partnership Self Assessment UTR must be valid" in {
      submitWithAuthorisedUser("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892")) {
        result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
      }
    }
  }

  "if the Ordinary Business Partnership form is successfully validated:" must {
    "the status code should be 200" in {
      submitWithAuthorisedUser("OBP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
        result =>
          status(result) must be(OK)
      }
    }
  }

  "if the Limited Liability Partnership form  is successfully validated:" must {
    "the status code should be 200" in {
      submitWithAuthorisedUser("LLP", request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111")) {
        result =>
          status(result) must be(OK)
      }
    }
  }

  "if the Sole Trader form  is successfully validated:" must {
    "the status code should be 200" in {
      submitWithAuthorisedUser("SOP", request.withFormUrlEncodedBody("firstName" -> "John", "lastName" -> "Smith", "saUTR" -> "1111111111")) {
        result =>
          status(result) must be(OK)
      }
    }
  }

  "if the Unincorporated body form  is successfully validated:" must {
    "the status code should be 200" in {
      submitWithAuthorisedUser("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(SEE_OTHER)
      }
    }
  }

  "if the Limited Company form  is successfully validated:" must {
    "the status code should be 200" in {
      submitWithAuthorisedUser("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co")) {
        result =>
          status(result) must be(OK)
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

  def submitWithAuthorisedUser(businessType : String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val matchSuccessResponse = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
    val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> matchSuccessResponse))
    when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
    when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}
