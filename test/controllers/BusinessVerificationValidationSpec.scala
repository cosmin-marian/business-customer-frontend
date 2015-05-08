package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector


class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"

  object TestBusinessVerificationController extends BusinessVerificationController  {
    val businessMatchingConnector = mockConnector
    val dataCacheConnector = mockDataCacheConnector
    val authConnector = mockAuthConnector
  }

  "if the selection is Unincorporated body :" must {
    "Business Name must not be empty" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Business Name must be entered")

      document.getElementById("businessName_field").text() must include("Business Name")
      document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
    }

    "CO Tax UTR must not be empty" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
    }


    "Business Name must not be more than 40 characters" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Business Name must not be more than 40 characters")
    }

    "CO Tax UTR must be 10 digits" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
    }

    "CO Tax UTR must be valid" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
    }
  }


  "if the selection is Limited Company :" must {
    "Business Name must not be empty" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Business Name must be entered")

      document.getElementById("businessName_field").text() must include("Business Name")
      document.getElementById("cotaxUTR_field").text() must include("COTAX Unique Tax Reference")
    }

    "CO Tax UTR must not be empty" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference must be entered")
    }


    "Business Name must not be more than 40 characters" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cotaxUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Business Name must not be more than 40 characters")
    }

    "CO Tax UTR must be 10 digits" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "11111111111"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference must be 10 digits")
    }

    "CO Tax UTR must be valid" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "cotaxUTR" -> "1234567892"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Corporation Tax Unique Tax Reference is not valid")
    }
  }


  "if the selection is Sole Trader:" must {
    "First name, last name and SA UTR  must be entered" in {
      val result = TestBusinessVerificationController.submit("ATED","SOP").apply(request.withFormUrlEncodedBody("first-name" -> "", "last-name" -> "", "saUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("First Name must be entered")
      contentAsString(result) must include("Last Name must be entered")
      contentAsString(result) must include("Self Assessment Unique Tax Reference must be entered")


      document.getElementById("first-name_field").text() must include("First Name")
      document.getElementById("last-name_field").text() must include("Last Name")
      document.getElementById("saUTR_field").text() must include("Self Assessment Unique Tax Reference")
    }

    "SA UTR must be valid" in {
      val result = TestBusinessVerificationController.submit("ATED","SOP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "saUTR" -> "1234567892"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Self Assessment Unique Tax Reference is not valid")
    }

    "First Name and Last Name must not be more than 40 characters" in {
      val result = TestBusinessVerificationController.submit("ATED","SOP").apply(request.withFormUrlEncodedBody("first-name" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "last-name" -> "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("First Name must not be more than 40 characters")
      contentAsString(result) must include("Last Name must not be more than 40 characters")
    }

    "SA  UTR must be 10 digits" in {
      val result = TestBusinessVerificationController.submit("ATED","SOP").apply(request.withFormUrlEncodedBody("first-name" -> "Smith & Co", "last-name" -> "Mohombi", "saUTR" -> "11111111111"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Self Assessment Unique Tax Reference must be 10 digits")
    }
  }

  "if the selection is Limited Liability Partnership:" must {
    "Business Name and CO Tax UTR must not be empty"  in {
      val result = TestBusinessVerificationController.submit("ATED","LLP").apply(request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Business Name must be entered")
      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
      document.getElementById("businessName_field").text() must include("Business Name")
      document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
    }

    "Business Name must not be more than 40 characters" in {
      val result = TestBusinessVerificationController.submit("ATED","LLP").apply(request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Business Name must not be more than 40 characters")
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      val result = TestBusinessVerificationController.submit("ATED","LLP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
    }

    "Partnership Self Assessment UTR must be valid" in {
      val result = TestBusinessVerificationController.submit("ATED","LLP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
    }
  }

  "if the selection is Ordinary Business Partnership :" must {
    "Business Name and CO Tax UTR must not be empty"  in {
      val result = TestBusinessVerificationController.submit("ATED","OBP").apply(request.withFormUrlEncodedBody("psaUTR" -> "", "businessName" -> ""))
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Business Name must be entered")
      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be entered")
      document.getElementById("businessName_field").text() must include("Business Name")
      document.getElementById("psaUTR_field").text() must include("Partnership Self Assessment Unique Tax Reference")
    }

    "Business Name must not be more than 40 characters" in {
      val result = TestBusinessVerificationController.submit("ATED","OBP").apply(request.withFormUrlEncodedBody("businessName" -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "psaUTR" -> ""))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Business Name must not be more than 40 characters")
    }

    "Partnership Self Assessment UTR must be 10 digits" in {
      val result = TestBusinessVerificationController.submit("ATED","OBP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "11111111111"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference must be 10 digits")
    }

    "Partnership Self Assessment UTR must be valid" in {
      val result = TestBusinessVerificationController.submit("ATED","OBP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1234567892"))
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include("Partnership Self Assessment Unique Tax Reference is not valid")
    }
  }

  "if the Ordinary Business Partnership form is successfully validated:" must {
    "the status code should be 200" in {
      val result = TestBusinessVerificationController.submit("ATED","OBP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111"))
      status(result) must be(OK)
    }
  }

  "if the Limited Liability Partnership form  is successfully validated:" must {
    "the status code should be 200" in {
      val result = TestBusinessVerificationController.submit("ATED","LLP").apply(request.withFormUrlEncodedBody("businessName" -> "Smith & Co", "psaUTR" -> "1111111111"))
      status(result) must be(OK)
    }
  }

  "if the Sole Trader form  is successfully validated:" must {
    "the status code should be 200" in {
      val result = TestBusinessVerificationController.submit("ATED","SOP").apply(request.withFormUrlEncodedBody("first-name" -> "John", "last-name" -> "Smith", "saUTR" -> "1111111111"))
      status(result) must be(OK)
    }
  }

  "if the Unincorporated body form  is successfully validated:" must {
    "the status code should be 200" in {
      val result = TestBusinessVerificationController.submit("ATED","UIB").apply(request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co"))
      status(result) must be(OK)
    }
  }

  "if the Limited Company form  is successfully validated:" must {
    "the status code should be 200" in {
      val result = TestBusinessVerificationController.submit("ATED","LTD").apply(request.withFormUrlEncodedBody("cotaxUTR" -> "1111111111", "businessName" -> "Smith & Co"))
      status(result) must be(OK)
    }
  }
}
