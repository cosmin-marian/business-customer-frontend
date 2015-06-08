package services
import connectors.{DataCacheConnector, BusinessCustomerConnector}
import models.{ReviewDetails, BusinessRegistration}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object BusinessRegistrationService extends BusinessRegistrationService  {

  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
  override val dataCacheConnector = DataCacheConnector
}

trait BusinessRegistrationService {
  val businessCustomerConnector: BusinessCustomerConnector
  val dataCacheConnector : DataCacheConnector

  def registerNonUk(registerData: BusinessRegistration)(implicit headerCarrier: HeaderCarrier) :Future[Option[ReviewDetails]] = {
    businessCustomerConnector.register(registerData).flatMap {
      registrationSuccessResponse => {
        val reviewDetails = registrationSuccessResponse.as[ReviewDetails]
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    }
  }
}
