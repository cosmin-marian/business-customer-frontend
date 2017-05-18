package connectors

import config.BusinessCustomerSessionCache
import models.{Address, BusinessRegistration, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class BusinessRegCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar  with BeforeAndAfterEach{

  val mockSessionCache = mock[SessionCache]

  case class FormData(name: String)

  object FormData {
    implicit val formats = Json.format[FormData]
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData = FormData("some-data")

  val formDataJson = Json.toJson(formData)

  val cacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  override def beforeEach: Unit = {
    reset(mockSessionCache)
  }

  object TestDataCacheConnector extends BusinessRegCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
    override val sourceId: String = ""
  }

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(BusinessCustomerSessionCache)
      }

      "return Some" when {
        "formId of the cached form does exist for defined data type" in {

          when(mockSessionCache.fetchAndGetEntry[FormData](key = Matchers.eq(formIdNotExist))(Matchers.any(), Matchers.any())) thenReturn {
            Future.successful(Some(formData))
          }
          await(TestDataCacheConnector.fetchAndGetBusinessRegForSession[FormData](formIdNotExist)) must be(Some(formData))
        }
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in {
        when(mockSessionCache.cache[FormData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(cacheMap)
        }
        await(TestDataCacheConnector.saveBusinessRegDetails[FormData](formId, formData)) must be(formData)
      }
    }
  }
}

