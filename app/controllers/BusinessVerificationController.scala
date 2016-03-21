package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessVerificationForms._
import forms._
import models.{Individual, Organisation, ReviewDetails}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{BCUtils, AuthUtils}
import utils.BusinessCustomerConstants._

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  override val businessMatchingService: BusinessMatchingService = BusinessMatchingService
  override val authConnector = FrontendAuthConnector
}

trait BusinessVerificationController extends BaseController {

  val businessMatchingService: BusinessMatchingService

  def businessVerification(service: String) =
    AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_verification(businessTypeForm, AuthUtils.isAgent, service, AuthUtils.isSaAccount(), AuthUtils.isOrgAccount()))
  }

  // scalastyle:off cyclomatic.complexity
  def continue(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest).fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.business_verification(formWithErrors, AuthUtils.isAgent, service, AuthUtils.isSaAccount(), AuthUtils.isOrgAccount()))),
        value => {
          value.businessType match {
            case Some("NUK") => Future.successful(Redirect(controllers.routes.BusinessRegController.register(service, "NUK")))
            case Some("NEW") => Future.successful(Redirect(controllers.routes.BusinessRegUKController.register(service, "NEW")))
            case Some("GROUP") => Future.successful(Redirect(controllers.routes.BusinessRegUKController.register(service, "GROUP")))
            case Some("SOP") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP")))
            case Some("UIB") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UIB")))
            case Some("LTD") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LTD")))
            case Some("OBP") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "OBP")))
            case Some("LLP") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LLP")))
            case Some("LP") => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LP")))
            case _ => Future.successful(Redirect(controllers.routes.HomeController.homePage(service)))
          }
        }
      )
  }

  def businessForm(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      businessType match {
        case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, AuthUtils.isAgent, service, businessType))
        case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, AuthUtils.isAgent, service, businessType))
        case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, AuthUtils.isAgent, service, businessType))
        case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, AuthUtils.isAgent, service, businessType))
        case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, AuthUtils.isAgent, service, businessType))
        case "LP" => Ok(views.html.business_lookup_LP(limitedPartnershipForm, AuthUtils.isAgent, service, businessType))
      }
  }

  def submit(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessType match {
        case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service)
        case "SOP" => sopFormHandling(soleTraderForm, businessType, service)
        case "LLP" => llpFormHandling(limitedLiabilityPartnershipForm, businessType, service)
        case "LP" => lpFormHandling(limitedPartnershipForm, businessType, service)
        case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service)
        case "LTD" => ltdFormHandling(limitedCompanyForm, businessType, service)
      }
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String,
                              service: String)(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, AuthUtils.isAgent, service, businessType))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UNINCORPORATED_BODY)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = AuthUtils.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = unincorporatedBodyForm.withError(key = "business-type-uib-form", message = errorMsg).fill(unincorporatedFormData)
                BadRequest(views.html.business_lookup_UIB(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String)
                             (implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, AuthUtils.isAgent, service, businessType))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = AuthUtils.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = soleTraderForm.withError(key = "business-type-sop-form", message = errorMsg).fill(soleTraderFormData)

                BadRequest(views.html.business_lookup_SOP(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String,
                              service: String)(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, AuthUtils.isAgent, service, businessType))),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, LLP)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = AuthUtils.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = limitedLiabilityPartnershipForm.withError(key = "business-type-llp-form", message = errorMsg).fill(llpFormData)
                BadRequest(views.html.business_lookup_LLP(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }

  private def lpFormHandling(limitedPartnershipForm: Form[LimitedPartnershipMatch], businessType: String, service: String)
                            (implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, AuthUtils.isAgent, service, businessType))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, PARTNERSHIP)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = AuthUtils.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = limitedPartnershipForm.withError(key = "business-type-lp-form", message = errorMsg).fill(lpFormData)
                BadRequest(views.html.business_lookup_LP(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String,
                              service: String)(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, AuthUtils.isAgent, service, businessType))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, PARTNERSHIP)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = AuthUtils.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = ordinaryBusinessPartnershipForm.withError(key = "business-type-obp-form", message = errorMsg).fill(obpFormData)
                BadRequest(views.html.business_lookup_OBP(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String,
                              service: String)(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, AuthUtils.isAgent, service, businessType))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CORPORATE_BODY)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = AuthUtils.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) map {
          returnedResponse => {
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(reviewDetailsValidated) => {
                Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
              case None => {
                val errorMsg = Messages("bc.business-verification-error.not-found")
                val errorForm = limitedCompanyForm.withError(key = "business-type-ltd-form", message = errorMsg).fill(limitedCompanyFormData)
                BadRequest(views.html.business_lookup_LTD(errorForm, AuthUtils.isAgent, service, businessType))
              }
            }
          }
        }
      }
    )
  }
}

