package services
import connectors.{DataCacheConnector, BusinessCustomerConnector}
import models._
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

    val nonUKRegisterDetails = createNonUKRegistrationRequest(registerData)
    businessCustomerConnector.registerNonUk(nonUKRegisterDetails).flatMap {
      registrationSuccessResponse => {
        val reviewDetails = createNonUKReviewDetails(registerData)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    }
  }


  private def createNonUKRegistrationRequest(registerData: BusinessRegistration) : NonUKRegistrationRequest = {
    val businessOrgData = EtmpOrganisation(organisationName = "testName")
    val nonUKIdentification = NonUKIdentification(idNumber = "id1", issuingInstitution="HRMC", issuingCountryCode = "UK")
    val businessAddress = EtmpAddress("line1", "line2", None, None, None, "GB")

    NonUKRegistrationRequest(
      acknowledgmentReference = "SESS:123123123",
      organisation = businessOrgData,
      address = AddressChoice(foreignAddress = businessAddress),
      isAnAgent = false,
      isAGroup = false,
      nonUKIdentification = nonUKIdentification
    )
  }

  private def createNonUKReviewDetails(registerData: BusinessRegistration) :ReviewDetails = {
    ReviewDetails(businessName = registerData.businessName, businessType = "", businessAddress = registerData.businessAddress)
  }
}
