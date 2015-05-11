package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.ReviewDetails
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, Org}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority, OrgAccount, PayeAccount}
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.http.{Upstream4xxResponse, SessionKeys}

import scala.concurrent.Future


class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController {
    override val businessMatchingConnector = mockBusinessMatchingConnector
    override val dataCacheConnector = mockDataCacheConnector
    override val authConnector = mockAuthConnector
  }

  "BusinessVerificationController" must {
    "use the correct authentication connector" in {
      controllers.BusinessVerificationController.authConnector must be(FrontendAuthConnector)
    }

    "use the correct business matching connector" in {
      controllers.BusinessVerificationController.businessMatchingConnector must be(BusinessMatchingConnector)
    }

    "use the correct data cache connector" in {
      controllers.BusinessVerificationController.dataCacheConnector must be(DataCacheConnector)
    }

    "respond to businessVerification" in {
      val result = route(FakeRequest(GET, "/business-customer/business-verification/ATED")).get
      status(result) must not be (NOT_FOUND)
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer/hello")).get
      status(result) must not be (NOT_FOUND)
    }

    "businessVerification" must {
      "authorised users" must {

        "respond with OK" in {

          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
          }
        }

        "return Business Verification view" in {

          getWithAuthorisedUser {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be("Business Verification")

              document.getElementById("business-verification-header").text() must be("Business verification")
              document.getElementById("business-lookup").text() must be("Business Lookup")
              document.select(".block-label").text() must include("Unincorporated Body")
              document.select(".block-label").text() must include("Limited Company")
              document.select(".block-label").text() must include("Sole Trader")
              document.select(".block-label").text() must include("Limited Liability Partnership")
              document.select(".block-label").text() must include("Partnership")
              document.select(".block-label").text() must include("Non UK-based Company")
              document.select("button").text() must be("Continue")
          }
        }
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }
    }
    "when selecting Sole Trader option" must {

      "add additional form fields to the screen for entry" in {
        getWithAuthorisedUser {
          result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("sole-trader-first-name_field").text() must be("First Name")
            document.getElementById("sole-trader-last-name_field").text() must be("Last Name")
            document.getElementById("sole-trader-utr_field").text() must be("Self Assessment Unique Tax Reference")
        }

      }

      "when selecting Limited Company option" must {

        "add additional form fields to the screen for entry" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)

              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("ltd-business-name_field").text() must be("Business Name")
              document.getElementById("ltd-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
          }
        }
      }

      "when selecting Unincorporated Body option" must {

        "add additional form fields to the screen for entry" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)

              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("uib-business-name_field").text() must be("Business Name")
              document.getElementById("uib-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
          }
        }
      }

      "when selecting Ordinary business partnership" must {

        "add additional form fields to the screen for entry" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("obp-business-name_field").text() must be("Business Name")
              document.getElementById("obp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
          }
        }
      }

      "when selecting Limited Liability Partnership option" must {

        "add additional form fields to the screen for entry" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("llp-business-name_field").text() must be("Business Name")
              document.getElementById("llp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
          }
        }
      }
    }

    "hello" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.helloWorld("").apply(FakeRequest())
        status(result) must be(OK)
      }

    }

    "submit" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          submitWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }


      "validate form" must {

        "if businessType is Sole Trader: FirstName, Surname and UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP")) { result =>
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              contentAsString(result) must include("First Name must be entered")
              contentAsString(result) must include("Last Name must be entered")
              contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")

              document.getElementById("sole-trader-first-name_field").text() must be("First Name")
              document.getElementById("sole-trader-last-name_field").text() must be("Last Name")
              document.getElementById("sole-trader-utr_field").text() must be("Self Assessment Unique Tax Reference")
            }
          }

          "if entered, First name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAFirstName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, Last name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sASurname" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, SA UTR must be 10 digits" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, SA UTR must be valid" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP", "soleTrader.sAUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
            }
          }
        }

        "if a Limited company: Business Name and COTAX UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))

                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

                document.getElementById("ltd-business-name_field").text() must be("Business Name")
                document.getElementById("ltd-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, COTAX UTR must be 10 digits" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdCotaxAUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, CO TAX UTR must be valid" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LTD", "ltdCompany.ltdCotaxAUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
            }
          }
        }

        "if an Unincorporated body: Business Name and COTAX UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")

                document.getElementById("uib-business-name_field").text() must be("Business Name")
                document.getElementById("uib-cotax-utr_field").text() must be("COTAX Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, COTAX UTR must be 10 digits" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibCotaxAUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, CO TAX UTR must be valid" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "UIB", "uibCompany.uibCotaxAUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
            }
          }
        }


        "if an Ordinary business partnership: Business Name and Partnership Self Assessment UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

                document.getElementById("obp-business-name_field").text() must be("Business Name")
                document.getElementById("obp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, Partnership UTR must be 10 digits" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpPSAUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, Partnership UTR must be valid" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "OBP", "obpCompany.obpPSAUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
            }
          }
        }

        "if Limited liability partnership: Business Name and Partnership Self Assessment UTR" must {

          "not be empty" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Business Name must be entered")
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")

                document.getElementById("llp-business-name_field").text() must be("Business Name")
                document.getElementById("llp-psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
            }
          }

          "if entered, Business Name must be less than 40 characters" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpBusinessName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Maximum length is 40")
            }
          }

          "if entered, Partnership UTR must be 10 digits" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpPSAUTR" -> "12345678917")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Unique Tax Reference must be 10 digits")
            }
          }

          "if entered, Partnership UTR must be valid" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "LLP", "llpCompany.llpPSAUTR" -> "1234567892")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val document = Jsoup.parse(contentAsString(result))
                contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
            }
          }
        }

        "if valid text has been entered - continue to next action - MATCH FOUND" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJsonForUIB = Json.parse( """{ "businessType": "UIB", "uibCompany": {"uibBusinessName": "ACME", "uibCotaxAUTR": "1111111111"} }""")

          val matchSuccessResponse = Json.parse( """{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
          val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> matchSuccessResponse))
          val successModel = ReviewDetails("ACME", "Unincorporated body", "23 High Street Park View The Park Gloucester Gloucestershire ABC 123", "201234567890", "contact@acme.com")

          when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.successful(successModel))
          when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

          submitWithAuthorisedUserJson(FakeRequest().withJsonBody(inputJsonForUIB)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/review-details/$service")
          }

        }

        "if valid text has been entered - continue to next action - MATCH NOT FOUND" in {
          val inputJsonForUIB = Json.parse( """{ "businessType": "UIB", "uibCompany": {"uibBusinessName": "ACME", "uibCotaxAUTR": "1111111112"} }""")

          when(mockBusinessMatchingConnector.lookup(Matchers.any())(Matchers.any())).thenReturn(Future.failed(Upstream4xxResponse("No business partner found", 404, 404)))

          submitWithAuthorisedUserJson(FakeRequest().withJsonBody(inputJsonForUIB)) {
            result =>
              an[Upstream4xxResponse] must be thrownBy await(result)
          }
        }

        "if empty" must {

          "return BadRequest" in {
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "")) {
              result =>
                status(result) must be(BAD_REQUEST)
            }
          }

        }

        "if non-uk, continue to the registration page" in {
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessType" -> "NUK")) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/business-customer/register")
          }
        }

      }
    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }

    val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestBusinessVerificationController.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserJson(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))
    }

    val result = TestBusinessVerificationController.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("CS100700A")))), None, None)
      Future.successful(Some(payeAuthority))
    }

    val result = TestBusinessVerificationController.submit(service).apply(FakeRequest().withFormUrlEncodedBody("businessType" -> "SOP").withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }
}


