package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._



class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController {
    val businessMatchingConnector = mockConnector
    val dataCacheConnector = mockDataCacheConnector
  }

  "if an Unincorporated body: Business Name" must {
    "not be empty" in {
      val result = TestBusinessVerificationController.submit().apply(request.withFormUrlEncodedBody("businessName" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Business Name must be entered")

      document.getElementById("business-name_field").text() must include("Business Name")
      document.getElementById("cotax-utr_field").text() must include("COTAX Unique Tax Reference")
    }

    "if an Unincorporated body: CO Tax UTR" must {
      "not be empty" in {
        val result = TestBusinessVerificationController.submit().apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> ""))
        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
      }

      //    "if entered, Business Name must be less than 40 characters" in {
      //      val result = TestBusinessVerificationController.submit().apply(request.withFormUrlEncodedBody("businessName"->"AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDD1"))
      //      status(result) must be(BAD_REQUEST)
      //
      //      val document = Jsoup.parse(contentAsString(result))
      //
      //      contentAsString(result) must include("Maximum length is 40")
      //    }
    }
  }
}
