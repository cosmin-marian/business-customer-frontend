package controllers

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import controllers.nonUKReg.{NRLQuestionController, BusinessRegController}
import forms.BusinessVerificationForms._
import forms._
import models._
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
  override val controllerId: String = "BusinessVerificationController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait BusinessVerificationController extends BackLinkController {

  def businessMatchingService: BusinessMatchingService

  def businessVerification(service: String) = AuthAction(service).async {
    implicit bcContext =>
      currentBackLink.map(backLink => Ok(views.html.business_verification(businessTypeForm, bcContext.user.isAgent, service, bcContext.user.isSa, bcContext.user.isOrg, backLink)))
  }

  // scalastyle:off cyclomatic.complexity
  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest, service).fold(
      formWithErrors =>
        currentBackLink.map(backLink => BadRequest(views.html.business_verification(formWithErrors, bcContext.user.isAgent, service, bcContext.user.isSa, bcContext.user.isOrg, backLink))
        ),
      value => {
        val returnCall = Some(routes.BusinessVerificationController.businessVerification(service).url)
        value.businessType match {
          case Some("NUK") if service.equals("capital-gains-tax") =>
            RedirectWithBackLink(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"), returnCall)
          case Some("NUK") =>
            RedirectWithBackLink(NRLQuestionController.controllerId, controllers.nonUKReg.routes.NRLQuestionController.view(service), returnCall)
          case Some("NEW") =>
            RedirectWithBackLink(BusinessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "NEW"), returnCall)
          case Some("GROUP") =>
            RedirectWithBackLink(BusinessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "GROUP"), returnCall)
          case Some("SOP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP")))
          case Some("UIB") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UIB")))
          case Some("LTD") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LTD")))
          case Some("OBP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "OBP")))
          case Some("LLP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LLP")))
          case Some("LP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LP")))
          case Some("UT") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UT")))
          case Some("NRL") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "NRL")))
          case _ =>
            RedirectWithBackLink(HomeController.controllerId, controllers.routes.HomeController.homePage(service), returnCall)
        }
      }
    )
  }

  def businessForm(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      businessType match {
        case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LP" => Ok(views.html.business_lookup_LP(limitedPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "UT" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "NRL" => Ok(views.html.business_lookup_NRL(nonResidentLandlordForm, bcContext.user.isAgent, service, businessType, getNrlBackLink(service)))
      }
  }

  def submit(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
    businessType match {
      case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service, backLink)
      case "SOP" => sopFormHandling(soleTraderForm, businessType, service, backLink)
      case "LLP" => llpFormHandling(limitedLiabilityPartnershipForm, businessType, service, backLink)
      case "LP" => lpFormHandling(limitedPartnershipForm, businessType, service, backLink)
      case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service, backLink)
      case "LTD" => ltdFormHandling(limitedCompanyForm, businessType, service, backLink)
      case "UT" => ltdFormHandling(limitedCompanyForm, businessType, service, backLink)
      case "NRL" => nrlFormHandling(nonResidentLandlordForm, businessType, service, getNrlBackLink(service))
    }
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UnincorporatedBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = unincorporatedBodyForm.withError(key = "business-type-uib-form", message = errorMsg).fill(unincorporatedFormData)
              Future.successful(BadRequest(views.html.business_lookup_UIB(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String, backLink: Option[String])
                             (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = bcContext.user.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = soleTraderForm.withError(key = "business-type-sop-form", message = errorMsg).fill(soleTraderFormData)
              Future.successful(BadRequest(views.html.business_lookup_SOP(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => currentBackLink.map(implicit backLink => BadRequest(views.html.business_lookup_LLP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedLiabilityPartnershipForm.withError(key = "business-type-llp-form", message = errorMsg).fill(llpFormData)
              Future.successful(BadRequest(views.html.business_lookup_LLP(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def lpFormHandling(limitedPartnershipForm: Form[LimitedPartnershipMatch], businessType: String, service: String, backLink: Option[String])
                            (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedPartnershipForm.withError(key = "business-type-lp-form", message = errorMsg).fill(lpFormData)
              Future.successful(BadRequest(views.html.business_lookup_LP(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = ordinaryBusinessPartnershipForm.withError(key = "business-type-obp-form", message = errorMsg).fill(obpFormData)
              Future.successful(BadRequest(views.html.business_lookup_OBP(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {

    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = limitedCompanyForm.withError(key = "business-type-ltd-form", message = errorMsg).fill(limitedCompanyFormData)
              Future.successful(BadRequest(views.html.business_lookup_LTD(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def nrlFormHandling(nrlForm: Form[NonResidentLandlordMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {

    nrlForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_NRL(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      nrlFormData => {
        val organisation = Organisation(nrlFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = nrlFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              val errorMsg = Messages("bc.business-verification-error.not-found")
              val errorForm = nonResidentLandlordForm.withError(key = "business-type-nrl-form", message = errorMsg).fill(nrlFormData)
              Future.successful(BadRequest(views.html.business_lookup_NRL(errorForm, bcContext.user.isAgent, service, businessType, backLink)))
          }
        }
      }
    )
  }

  private def getNrlBackLink(service: String) = Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)
}
