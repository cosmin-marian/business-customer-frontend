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
  val nonUKBusinessType = "Non UK-based Company"
}

trait BusinessRegistrationService {

  def businessCustomerConnector: BusinessCustomerConnector

  def dataCacheConnector: DataCacheConnector

  def nonUKBusinessType: String

  def registerBusiness(registerData: BusinessRegistration,
                       isGroup: Boolean,
                       isNonUKClientRegisteredByAgent: Boolean = false,
                       service: String,
                       isBusinessDetailsEditable: Boolean = false)
                      (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[ReviewDetails] = {

    val businessRegisterDetails = createBusinessRegistrationRequest(registerData, isGroup, isNonUKClientRegisteredByAgent)

    for {
      registerResponse <- businessCustomerConnector.register(businessRegisterDetails, service, isNonUKClientRegisteredByAgent)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse.sapNumber,
          registerResponse.safeId, registerResponse.agentReferenceNumber, isGroup, registerData, isBusinessDetailsEditable)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.registration-failed")))
    }
  }


  def updateRegisterBusiness(registerData: BusinessRegistration,
                             isGroup: Boolean,
                             isNonUKClientRegisteredByAgent: Boolean = false,
                             service: String,
                             isBusinessDetailsEditable: Boolean = false)
                            (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[ReviewDetails] = {

    val updateRegisterDetails = createUpdateBusinessRegistrationRequest(registerData, isGroup, isNonUKClientRegisteredByAgent)

    for {
      oldReviewDetials <- dataCacheConnector.fetchAndGetBusinessDetailsForSession
      registerResponse <-
      oldReviewDetials match {
        case Some(reviewDetails) => businessCustomerConnector.updateRegistrationDetails(reviewDetails.safeId, updateRegisterDetails)
        case _ => throw new InternalServerException(Messages("bc.connector.error.update-registration-failed"))
      }
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse.sapNumber, registerResponse.safeId,
          registerResponse.agentReferenceNumber, isGroup, registerData,
          isBusinessDetailsEditable)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.update-registration-failed")))
    }
  }


  def getDetails()(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Option[(String, BusinessRegistration)]] = {

    def createBusinessRegistration(reviewDetailsOpt: Option[ReviewDetails]) : Option[(String, BusinessRegistration)] = {
      reviewDetailsOpt.flatMap( details =>
        details.businessType.map{ busType =>

          (busType, BusinessRegistration(details.businessName,
            details.businessAddress,
            Some(details.identification.isDefined),
            details.identification.map(_.idNumber),
            details.identification.map(_.issuingInstitution),
            details.identification.map(_.issuingCountryCode))
            )
        }
      )
    }
    dataCacheConnector.fetchAndGetBusinessDetailsForSession.map( createBusinessRegistration(_) )
  }




  private def createUpdateBusinessRegistrationRequest(registerData: BusinessRegistration,
                                                      isGroup: Boolean,
                                                      isNonUKClientRegisteredByAgent: Boolean = false)
                                                     (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): UpdateRegistrationDetailsRequest = {

    UpdateRegistrationDetailsRequest(
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      isAnIndividual = false,
      individual = None,
      organisation = Some(EtmpOrganisation(organisationName = registerData.businessName)),
      address = getEtmpBusinessAddress(registerData.businessAddress),
      contactDetails = EtmpContactDetails(),
      isAnAgent = if (isNonUKClientRegisteredByAgent) false else bcContext.user.isAgent,
      isAGroup = isGroup,
      identification = getEtmpIdentification(registerData)
    )
  }

  private def createBusinessRegistrationRequest(registerData: BusinessRegistration,
                                                isGroup: Boolean,
                                                isNonUKClientRegisteredByAgent: Boolean = false)
                                               (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): BusinessRegistrationRequest = {

    BusinessRegistrationRequest(
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      organisation = EtmpOrganisation(organisationName = registerData.businessName),
      address = getEtmpBusinessAddress(registerData.businessAddress),
      isAnAgent = if (isNonUKClientRegisteredByAgent) false else bcContext.user.isAgent,
      isAGroup = isGroup,
      identification = getEtmpIdentification(registerData),
      contactDetails = EtmpContactDetails()
    )
  }


  private def createReviewDetails(sapNumber: String, safeId: String,
                                  agentReferenceNumber: Option[String],
                                  isGroup: Boolean,
                                  registerData: BusinessRegistration,
                                  isBusinessDetailsEditable: Boolean): ReviewDetails = {

    val identification = registerData.businessUniqueId.map( busUniqueId =>
      Identification(busUniqueId,
        registerData.issuingInstitution.getOrElse(""),
        registerData.issuingCountry.getOrElse("")
      )
    )

    ReviewDetails(businessName = registerData.businessName,
      businessType = Some(nonUKBusinessType),
      businessAddress = registerData.businessAddress,
      sapNumber = sapNumber,
      safeId = safeId,
      isAGroup = isGroup,
      agentReferenceNumber = agentReferenceNumber,
      identification = identification,
      isBusinessDetailsEditable = isBusinessDetailsEditable
    )
  }


  private def getEtmpBusinessAddress(businessAddress: Address) = {
    EtmpAddress(addressLine1 = businessAddress.line_1,
      addressLine2 = businessAddress.line_2,
      addressLine3 = businessAddress.line_3,
      addressLine4 = businessAddress.line_4,
      postalCode = businessAddress.postcode,
      countryCode = businessAddress.country)
  }

  private def getEtmpIdentification(registerData: BusinessRegistration) = {
    if (registerData.businessUniqueId.isDefined || registerData.issuingInstitution.isDefined) {
      Some(EtmpIdentification(idNumber = registerData.businessUniqueId.getOrElse(""),
        issuingInstitution = registerData.issuingInstitution.getOrElse(""),
        issuingCountryCode = registerData.issuingCountry.getOrElse(registerData.businessAddress.country)))
    } else {
      None
    }
  }
}
