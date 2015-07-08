package services

import java.util.UUID

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
import utils.AuthUtils
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


  def registerNonUk(registerData: BusinessRegistration)(implicit user: AuthContext, headerCarrier: HeaderCarrier) :Future[ReviewDetails] = {

    val nonUKRegisterDetails = createNonUKRegistrationRequest(registerData)

    for {
      registerResponse <- businessCustomerConnector.registerNonUk(nonUKRegisterDetails)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse, registerData)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      auditRegisterBusiness(registerData)
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.registration-failed")))
    }
  }


  private def createNonUKRegistrationRequest(registerData: BusinessRegistration)
                                            (implicit user: AuthContext, headerCarrier: HeaderCarrier): NonUKRegistrationRequest = {

    val businessOrgData = EtmpOrganisation(organisationName = registerData.businessName)

    val nonUKIdentification = {
      if (registerData.businessUniqueId.isDefined || registerData.issuingInstitution.isDefined) {
        Some(NonUKIdentification(idNumber = registerData.businessUniqueId,
          issuingInstitution= registerData.issuingInstitution,
          issuingCountryCode = Some(registerData.businessAddress.country)))
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

    NonUKRegistrationRequest(
      acknowledgmentReference = sessionOrUUID,
      organisation = businessOrgData,
      address = businessAddress,
      isAnAgent = AuthUtils.isAgent,
      isAGroup = false,
      nonUKIdentification = nonUKIdentification
    )
  }

  private def createReviewDetails(response: NonUKRegistrationResponse,
                                  registerData: BusinessRegistration): ReviewDetails = {

    ReviewDetails(businessName = registerData.businessName,
      businessType = nonUKbusinessType,
      businessAddress = registerData.businessAddress,
      sapNumber = response.sapNumber,
      safeId  = response.safeId,
      agentReferenceNumber = response.agentReferenceNumber
    )
  }

  private def sessionOrUUID(implicit hc: HeaderCarrier): String = {
    hc.sessionId match {
      case Some(sessionId) => sessionId.value
      case None => UUID.randomUUID().toString.replace("-", "")
    }
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
