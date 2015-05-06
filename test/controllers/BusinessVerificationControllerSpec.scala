package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future


class BusinessVerificationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController {
    val businessMatchingConnector = mockConnector
    val dataCacheConnector = mockDataCacheConnector
  }

  "BusinessVerificationController" must {

    "respond to businessVerification" in {
      val result = route(FakeRequest(GET, "/business-customer/business-verification/ATED")).get
      status(result) must not be (NOT_FOUND)
    }

    "respond to hello" in {
      val result = route(FakeRequest(GET, "/business-customer/hello")).get
      status(result) must not be (NOT_FOUND)
    }

    "businessVerification" must {

      "respond with OK" in {
        val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest())
        status(result) must be(OK)
      }

      "return Business Verification view" in {

        val result = TestBusinessVerificationController.businessVerification(service).apply(FakeRequest())

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be("Business Verification")
        document.getElementById("business-verification-header").text() must be("Business verification")

        document.select(".block-label").text() must include("Unincorporated Body")
        document.select(".block-label").text() must include("Limited Company")
        document.select(".block-label").text() must include("Sole Trader")
        document.select(".block-label").text() must include("Limited Liability Partnership")
        document.select(".block-label").text() must include("Partnership")
        document.select(".block-label").text() must include("Non UK-based Company")
        document.select("button").text() must be("Continue")
      }
    }
    "when selecting Sole Trader option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "SOP"}""")))
        status(result) must be(303)
        redirectLocation(result).get must include("/business-verification/ATED/lookup")
      }

      "sole trader screen should add additional form fields" in {
        val result = TestBusinessVerificationController.businessLookup(service, "SOP").apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("first-name_field").text() must be("First Name")
        document.getElementById("last-name_field").text() must be("Last Name")
        document.getElementById("sa-utr_field").text() must be("Self Assessment Unique Tax Reference")
      }
    }

    "when selecting Limited Company option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LTD"}""")))
        status(result) must be(303)
        redirectLocation(result).get must include("/business-verification/ATED/lookup")
      }

      "limited company screen should add additional form fields" in {
        val result = TestBusinessVerificationController.businessLookup(service, "LTD").apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("business-name_field").text() must be("Business Name")
        document.getElementById("cotax-utr_field").text() must be("COTAX Unique Tax Reference")
      }
    }

    "when selecting Unincorporated Body option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UIB"}""")))
        status(result) must be(303)
        redirectLocation(result).get must include("/business-verification/ATED/lookup")
      }

      "Unincorporated body screen should add additional form fields" in {
        val result = TestBusinessVerificationController.businessLookup(service, "UIB").apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("business-name_field").text() must be("Business Name")
        document.getElementById("cotax-utr_field").text() must be("COTAX Unique Tax Reference")
      }
    }

    "when selecting Ordinary business partnership" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "OBP"}""")))
        status(result) must be(303)
        redirectLocation(result).get must include("/business-verification/ATED/lookup")
      }

      "Ordinary business partnership screen should add additional form fields" in {
        val result = TestBusinessVerificationController.businessLookup(service, "OBP").apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("business-name_field").text() must be("Business Name")
        document.getElementById("psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")
      }
    }

    "when selecting Limited Liability Partnership option" must {

      "redirect to next screen to allow additional form fields to be entered" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LLP"}""")))
        status(result) must be(303)
        redirectLocation(result).get must include("/business-verification/ATED/lookup")
      }

      "Limited liability partnership screen should add additional form fields" in {
        val result = TestBusinessVerificationController.businessLookup(service, "LLP").apply(FakeRequest())
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        println("!!!!!!!!!!!!!!!!" +document)
        document.getElementById("business-name_field").text() must be("Business Name")
        document.getElementById("psa-utr_field").text() must be("Partnership Self Assessment Unique Tax Reference")

      }
    }

      "if empty" must {

        "return BadRequest" in {
          val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : ""}""")))
          status(result) must be(BAD_REQUEST)
        }
      }

      "if non-uk, continue to registration page" in {
        val result = TestBusinessVerificationController.continue(service).apply(FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NUK"}""")))
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/register")
      }
    }
  }
