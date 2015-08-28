package services



import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import connectors.{BusinessCustomerConnector, DataCacheConnector}
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.InternalServerException
import utils.{SessionUtils, AuthUtils}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object BusinessRegistrationService extends BusinessRegistrationService  {

  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
  val dataCacheConnector = DataCacheConnector
  val nonUKbusinessType = "Non UK-based Company"
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}

trait BusinessRegistrationService extends Auditable {
  val businessCustomerConnector: BusinessCustomerConnector
  val dataCacheConnector: DataCacheConnector
  val nonUKbusinessType: String


  def registerBusiness(registerData: BusinessRegistration, isGroup: Boolean)(implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[ReviewDetails] = {

    val businessRegisterDetails = createBusinessRegistrationRequest(registerData, isGroup)

    for {
      registerResponse <- businessCustomerConnector.registerNonUk(businessRegisterDetails)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse, registerData)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      auditRegisterBusiness(registerData)
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.registration-failed")))
    }
  }


  private def createBusinessRegistrationRequest(registerData: BusinessRegistration, isGroup: Boolean)
                                            (implicit user: AuthContext, headerCarrier: HeaderCarrier): BusinessRegistrationRequest = {

    val businessOrgData = EtmpOrganisation(organisationName = registerData.businessName)

    val businessIdentification = {
      if (registerData.businessUniqueId.isDefined || registerData.issuingInstitution.isDefined) {
        Some(EtmpIdentification(idNumber = registerData.businessUniqueId.getOrElse(""),
          issuingInstitution= registerData.issuingInstitution.getOrElse(""),
          issuingCountryCode = registerData.businessAddress.country))
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
      isAnAgent = AuthUtils.isAgent,
      isAGroup = isGroup,
      identification = businessIdentification,
      contactDetails = EtmpContactDetails()
    )
  }

  private def createReviewDetails(response: BusinessKRegistrationResponse,
                                  registerData: BusinessRegistration): ReviewDetails = {

    ReviewDetails(businessName = registerData.businessName,
      businessType = Some(nonUKbusinessType),
      businessAddress = registerData.businessAddress,
      sapNumber = response.sapNumber,
      safeId  = response.safeId,
      agentReferenceNumber = response.agentReferenceNumber
    )
  }


  private def auditRegisterBusiness(registerData: BusinessRegistration)(implicit hc: HeaderCarrier) = {
    sendDataEvent("registerNonUk", detail = Map(
      "txName" -> "registerNonUk",
      "businessName" -> registerData.businessName,
      "businessAddressLine1" -> registerData.businessAddress.line_1,
      "businessUniqueId" -> registerData.businessUniqueId.getOrElse(""),
      "issuingInstitution" -> registerData.issuingInstitution.getOrElse(""))
    )
  }
}
