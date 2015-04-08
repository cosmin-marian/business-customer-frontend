package controllers

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future


class BusinessVerificationTest extends UnitSpec with WithFakeApplication {

  val request = FakeRequest()

  "Calling Business Verification with no session" should {
    "return a 200 response" in  {

      val controllerUnderTest = BusinessVerification
      val result = controllerUnderTest.show(request)

      status(result) shouldBe 200
    }

    "return Business Verification view" in  {

      val controllerUnderTest = BusinessVerification
      val result = Future.successful(controllerUnderTest.show(request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("business-verification-header").text() shouldBe "Business verification"
      document.getElementById("business-lookup").text() shouldBe "Business Lookup"
      document.getElementsByAttribute(".block-label").text() shouldBe "Unincorporated body"
      document.getElementsByAttribute(".block-label").text() shouldBe "Limited company"
      document.getElementsByAttribute(".block-label").text() shouldBe "Sole proprietor"
      document.getElementsByAttribute(".block-label").text() shouldBe "Limited liability partnership"
      document.getElementsByAttribute(".block-label").text() shouldBe "Partnership"
    }
  }
}
