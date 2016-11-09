package services

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import controllers.auth.ExternalUrls
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.play.http.{HttpResponse, BadRequestException, HeaderCarrier, InternalServerException}
import utils.{BusinessCustomerConstants, SessionUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import play.mvc.Http.Status._
import play.api.libs.json._
import play.api.libs.json.Reads._
object BusinessRegistrationService extends BusinessRegistrationService {

  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
  val dataCacheConnector = DataCacheConnector
  val nonUKBusinessType = "Non UK-based Company"
}

trait BusinessRegistrationService {

  def businessCustomerConnector: BusinessCustomerConnector

  def dataCacheConnector: DataCacheConnector

  def nonUKBusinessType: String

  def registerBusiness(registerData: BusinessRegistration, isGroup: Boolean, isNonUKClientRegisteredByAgent: Boolean = false, service: String)
                      (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[ReviewDetails] = {

    val businessRegisterDetails = createBusinessRegistrationRequest(registerData, isGroup, isNonUKClientRegisteredByAgent)

    for {
      registerResponse <- businessCustomerConnector.register(businessRegisterDetails, service, isNonUKClientRegisteredByAgent)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse, isGroup, registerData)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException(Messages("bc.connector.error.registration-failed")))
    }
  }


  def getDetails()(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[(Option[String], Option[BusinessRegistration])] = {

    def createBusinessRegistration(response: HttpResponse) : BusinessRegistration = {

      val businessName = (response.json \ "organisation" \ "organisationName").as[String]

      val businessAddress = Address(
        (response.json \ "addressDetails" \ "addressLine1").as[String],
        (response.json \ "addressDetails" \ "addressLine2").as[String],
        (response.json \ "addressDetails" \ "addressLine3").asOpt[String],
        (response.json \ "addressDetails" \ "addressLine4").asOpt[String],
        (response.json \ "addressDetails" \ "postalCode").asOpt[String],
        (response.json \ "addressDetails" \ "countryCode").as[String]
      )

      val businessUniqueId = (response.json \ "nonUKIdentification" \ "idNumber").asOpt[String]
      val hasBusinessUniqueId = businessUniqueId.map(id => true)
      val issuingInstitution = (response.json \ "nonUKIdentification" \ "issuingInstitution").asOpt[String]
      val issuingCountry = (response.json \ "nonUKIdentification" \ "issuingCountryCode").asOpt[String]

      BusinessRegistration(businessName,
        businessAddress,
        hasBusinessUniqueId,
        businessUniqueId,
        issuingInstitution,
        issuingCountry)
    }

    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap { reviewDetailsOpt =>
      reviewDetailsOpt match {
        case Some(reviewDetails) =>
          businessCustomerConnector.getDetails(identifier = reviewDetails.safeId, identifierType = BusinessCustomerConstants.IdentifierSafeId) map {
            response =>
              response.status match {
                case OK => (reviewDetails.businessType, Some(createBusinessRegistration(response)))
                case NOT_FOUND => (reviewDetails.businessType, None)
                case BAD_REQUEST =>
                  Logger.warn(s"[DetailsService][getDetails] status = ${response.status} - body = ${response.body}")
                  throw new BadRequestException(s"[BusinessRegistrationService][getDetails] Bad Data, " +
                    s"status = ${response.status} - body = ${response.body}")
                case _ =>
                  Logger.warn(s"[DetailsService][getDetails] status = ${response.status} - body = ${response.body}")
                  throw new InternalServerException(s"[BusinessRegistrationService][getDetails] Internal server error, " +
                    s"status = ${response.status} - body = ${response.body}")
              }
          }
        case _ => throw new RuntimeException("SafeId not found")
      }
    }
  }

  private def createBusinessRegistrationRequest(registerData: BusinessRegistration,
                                                isGroup: Boolean,
                                                isNonUKClientRegisteredByAgent: Boolean = false)
                                               (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): BusinessRegistrationRequest = {
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
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      organisation = businessOrgData,
      address = businessAddress,
      isAnAgent = if (isNonUKClientRegisteredByAgent) false else bcContext.user.isAgent,
      isAGroup = isGroup,
      identification = businessIdentification,
      contactDetails = EtmpContactDetails()
    )
  }

  private def createReviewDetails(response: BusinessRegistrationResponse, isGroup: Boolean,
                                  registerData: BusinessRegistration): ReviewDetails = {

    ReviewDetails(businessName = registerData.businessName,
      businessType = Some(nonUKBusinessType),
      businessAddress = registerData.businessAddress,
      sapNumber = response.sapNumber,
      safeId = response.safeId,
      isAGroup = isGroup,
      agentReferenceNumber = response.agentReferenceNumber
    )
  }

}
