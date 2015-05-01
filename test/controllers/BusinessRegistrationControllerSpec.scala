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

class BusinessRegistrationControllerSpec extends PlaySpec with OneServerPerSuite{

  val request = FakeRequest()
  val service = "ATED"

  object TestBusinessRegController extends BusinessRegController {



    }

  "BusinessRegController" must {

    "respond to /" in {
      val result = route(FakeRequest(GET, "/business-customer")).get
      status(result) must not be (NOT_FOUND)
    }


   "return business registration view" in {

     val result = TestBusinessRegController.register().apply(FakeRequest())
     val document = Jsoup.parse(contentAsString(result))

      document.title() must be("Business Registration")
      document.getElementById("business-registration-header").text() must be("Add business details")


  }


  }
}
