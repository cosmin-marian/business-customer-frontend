package services

import connectors.{DataCacheConnector, BusinessCustomerConnector}
import models.{NonUKRegistrationResponse, NonUKRegistrationRequest}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class BusinessRegistrationServiceSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestBusinessRegistrationService extends BusinessRegistrationService {
    override val businessCustomerConnector: BusinessCustomerConnector = TestConnector
    override val dataCacheConnector = DataCacheConnector
  }

  object TestConnector extends BusinessCustomerConnector {
    override def registerNonUk(registerData: NonUKRegistrationRequest)(implicit headerCarrier: HeaderCarrier): Future[Option[NonUKRegistrationResponse]] = {
      val nonUKResponse =  NonUKRegistrationResponse(processingDate = "2015-01-01",
        sapNumber = "SAP123123",
        safeId = "SAFE123123",
        agentReferenceNumber = "AREF123123")

      Future(Some(nonUKResponse))
    }
  }

}
