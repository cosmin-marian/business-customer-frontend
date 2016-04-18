package services

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.SessionUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessRegistrationService extends BusinessRegistrationService {

  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
  val dataCacheConnector = DataCacheConnector
  val nonUKbusinessType = "Non UK-based Company"
}

trait BusinessRegistrationService {
  val businessCustomerConnector: BusinessCustomerConnector
  val dataCacheConnector: DataCacheConnector
  val nonUKbusinessType: String


  def registerBusiness(registerData: BusinessRegistration, isGroup: Boolean)(implicit businessCustomerContext: BusinessCustomerContext, hc: HeaderCarrier): Future[ReviewDetails] = {

    val businessRegisterDetails = createBusinessRegistrationRequest(registerData, isGroup)

    for {
      registerResponse <- businessCustomerConnector.register(businessRegisterDetails)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse, isGroup, registerData)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.registration-failed")))
    }
  }


  private def createBusinessRegistrationRequest(registerData: BusinessRegistration, isGroup: Boolean)
                                               (implicit businessCustomerContext: BusinessCustomerContext, hc: HeaderCarrier): BusinessRegistrationRequest = {
    val businessOrgData = EtmpOrganisation(organisationName = registerData.businessName)

    val businessIdentification = {
      if (registerData.businessUniqueId.isDefined || registerData.issuingInstitution.isDefined) {
        Some(EtmpIdentification(idNumber = registerData.businessUniqueId.getOrElse(""),
          issuingInstitution = registerData.issuingInstitution.getOrElse(""),
          issuingCountryCode = registerData.issuingCountry.getOrElse(registerData.businessAddress.country)))
      } else {
        None
      }
    }

    val businessAddress = EtmpAddress(addressLine1 = registerData.businessAddress.line_1,
      addressLine2 = registerData.businessAddress.line_2,
      addressLine3 = registerData.businessAddress.line_3,
      addressLine4 = registerData.businessAddress.line_4,
      postalCode = registerData.businessAddress.postcode,
      countryCode = registerData.businessAddress.country)

    BusinessRegistrationRequest(
      acknowledgmentReference = SessionUtils.getUniqueAckNo,
      organisation = businessOrgData,
      address = businessAddress,
      isAnAgent = businessCustomerContext.user.isAgent,
      isAGroup = isGroup,
      identification = businessIdentification,
      contactDetails = EtmpContactDetails()
    )
  }

  private def createReviewDetails(response: BusinessRegistrationResponse, isGroup: Boolean,
                                  registerData: BusinessRegistration): ReviewDetails = {

    ReviewDetails(businessName = registerData.businessName,
      businessType = Some(nonUKbusinessType),
      businessAddress = registerData.businessAddress,
      sapNumber = response.sapNumber,
      safeId = response.safeId,
      isAGroup = isGroup,
      agentReferenceNumber = response.agentReferenceNumber
    )
  }

}
