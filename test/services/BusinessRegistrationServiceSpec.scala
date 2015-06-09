package services

import connectors.{DataCacheConnector, BusinessCustomerConnector}
import models.{NonUKRegistrationResponse, NonUKRegistrationRequest}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class BusinessRegistrationServiceSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestBusinessRegistrationService extends BusinessRegistrationService {
    val businessCustomerConnector: BusinessCustomerConnector = TestConnector
    val dataCacheConnector = DataCacheConnector
    val issuingInstitution = "HMRC"
    val issuingCountryCode = "UK"
    val nonUKbusinessType = "Non UK-based Company"
  }

  object TestConnector extends BusinessCustomerConnector {
    override def registerNonUk(registerData: NonUKRegistrationRequest)(implicit headerCarrier: HeaderCarrier): Future[NonUKRegistrationResponse] = {
      val nonUKResponse =  NonUKRegistrationResponse(processingDate = "2015-01-01",
        sapNumber = "SAP123123",
        safeId = "SAFE123123",
        agentReferenceNumber = "AREF123123")

      Future(nonUKResponse)
    }
  }

  "BusinessRegistrationService" must {

    "use the correct data cache connector" in {
      BusinessRegistrationService.dataCacheConnector must be(DataCacheConnector)
    }

    "use the correct business Customer Connector" in {
      BusinessRegistrationService.businessCustomerConnector must be(BusinessCustomerConnector)
    }
  }
}
