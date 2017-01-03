package controllers

import config.FrontendAuthConnector
import forms.BusinessVerificationForms._
import forms._
import models.{BusinessCustomerContext, Individual, Organisation, ReviewDetails}
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.BusinessCustomerConstants._

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  val businessMatchingService: BusinessMatchingService = BusinessMatchingService
  val authConnector = FrontendAuthConnector
}

trait BusinessVerificationController extends BaseController {

  def businessMatchingService: BusinessMatchingService

  def businessVerification(service: String) = AuthAction(service) {
    implicit bcContext =>
      Ok(views.html.business_verification(businessTypeForm, bcContext.user.isAgent, service, bcContext.user.isSa, bcContext.user.isOrg))
  }

  // scalastyle:off cyclomatic.complexity
  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest).fold(
      formWithErrors =>
        BadRequest(views.html.business_verification(formWithErrors, bcContext.user.isAgent, service,
          bcContext.user.isSa, bcContext.user.isOrg)),
      value => {
        value.businessType match {
          case Some("NUK") => Redirect(controllers.routes.NRLQuestionController.view(service))
          case Some("NEW") => Redirect(controllers.routes.BusinessRegUKController.register(service, "NEW"))
          case Some("GROUP") => Redirect(controllers.routes.BusinessRegUKController.register(service, "GROUP"))
          case Some("SOP") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
          case Some("UIB") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UIB"))
          case Some("LTD") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LTD"))
          case Some("OBP") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "OBP"))
          case Some("LLP") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LLP"))
          case Some("LP") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LP"))
          case Some("UT") => Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UT"))
          case _ => Redirect(controllers.routes.HomeController.homePage(service))
        }
      }
    )
  }

  def businessForm(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
    businessType match {
      case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, bcContext.user.isAgent, service, businessType))
      case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType))
      case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, bcContext.user.isAgent, service, businessType))
      case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, bcContext.user.isAgent, service, businessType))
      case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, bcContext.user.isAgent, service, businessType))
      case "LP" => Ok(views.html.business_lookup_LP(limitedPartnershipForm, bcContext.user.isAgent, service, businessType))
      case "UT" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType))
    }
  }

  def submit(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    businessType match {
      case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service)
      case "SOP" => sopFormHandling(soleTraderForm, businessType, service)
      case "LLP" => llpFormHandling(limitedLiabilityPartnershipForm, businessType, service)
      case "LP" => lpFormHandling(limitedPartnershipForm, businessType, service)
      case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service)
      case "LTD" => ltdFormHandling(limitedCompanyForm, businessType, service)
      case "UT" => ltdFormHandling(limitedCompanyForm, businessType, service)
    }
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String,
                              service: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, bcContext.user.isAgent, service, businessType))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UnincorporatedBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = unincorporatedBodyForm.withError(key = "business-type-uib-form", message = errorMsg).fill(unincorporatedFormData)
              BadRequest(views.html.business_lookup_UIB(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String)
                             (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, bcContext.user.isAgent, service, businessType))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = bcContext.user.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = soleTraderForm.withError(key = "business-type-sop-form", message = errorMsg).fill(soleTraderFormData)
              BadRequest(views.html.business_lookup_SOP(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String,
                              service: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, bcContext.user.isAgent, service, businessType))),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedLiabilityPartnershipForm.withError(key = "business-type-llp-form", message = errorMsg).fill(llpFormData)
              BadRequest(views.html.business_lookup_LLP(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

  private def lpFormHandling(limitedPartnershipForm: Form[LimitedPartnershipMatch], businessType: String, service: String)
                            (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, bcContext.user.isAgent, service, businessType))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedPartnershipForm.withError(key = "business-type-lp-form", message = errorMsg).fill(lpFormData)
              BadRequest(views.html.business_lookup_LP(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String,
                              service: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, bcContext.user.isAgent, service, businessType))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = ordinaryBusinessPartnershipForm.withError(key = "business-type-obp-form", message = errorMsg).fill(obpFormData)
              BadRequest(views.html.business_lookup_OBP(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String,
                              service: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, bcContext.user.isAgent, service, businessType))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) map { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedCompanyForm.withError(key = "business-type-ltd-form", message = errorMsg).fill(limitedCompanyFormData)
              BadRequest(views.html.business_lookup_LTD(errorForm, bcContext.user.isAgent, service, businessType))
          }
        }
      }
    )
  }

}
