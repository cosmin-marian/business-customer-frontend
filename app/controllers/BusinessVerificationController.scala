package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessVerificationForms._
import forms._
import models.{MatchBusinessData, Individual, Organisation, ReviewDetails}
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  override val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
}

trait BusinessVerificationController extends FrontendController with Actions {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def businessVerification(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_verification(businessTypeForm, service))
  }

  def continue(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessTypeForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors, service))),
        value => {
          value.businessType match {
            case "NUK" => Future.successful(Redirect(controllers.routes.BusinessRegController.register(service)))
            case "SOP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP")))
            case "UIB" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UIB")))
            case "LTD" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LTD")))
            case "OBP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "OBP")))
            case "LLP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LLP")))
            case "LP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LP")))
          }
        }
      )
  }

  def businessForm(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      businessType match {
        case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, service, businessType))
        case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, service, businessType))
        case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, service ,businessType))
        case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, service, businessType))
        case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, service, businessType))
        case "LP" =>  Ok(views.html.business_lookup_LP(limitedPartnershipForm, service, businessType))
      }
  }

  def submit(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
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

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String,
                              service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, service, businessType))),
      unincorporatedFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = unincorporatedFormData.cotaxUTR, requiresNameMatch = false, isAnAgent = false,
          individual = None, organisation = Some(Organisation(unincorporatedFormData.businessName, businessType)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String
                               )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, service, businessType))),
      soleTraderFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = soleTraderFormData.saUTR, requiresNameMatch = false, isAnAgent = false,
          individual = Some(Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)), organisation = None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String,
                              service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, service, businessType))),
      llpFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = llpFormData.psaUTR, requiresNameMatch = false, isAnAgent = false,
          individual = None, organisation = Some(Organisation(llpFormData.businessName, businessType)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def lpFormHandling(limitedPartnershipForm: Form[LimitedPartnershipMatch], businessType: String, service:
    String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, service, businessType))),
      lpFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = lpFormData.psaUTR, requiresNameMatch = false, isAnAgent = false,
          individual = None, organisation = Some(Organisation(lpFormData.businessName, businessType)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String,
                              service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, service, businessType))),
      obpFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = obpFormData.psaUTR, requiresNameMatch = false, isAnAgent = false,
          individual = None, organisation = Some(Organisation(obpFormData.businessName, businessType)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String,
                              service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, service, businessType))),
      limitedCompanyFormData => {
        val businessDetails = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = limitedCompanyFormData.cotaxUTR, requiresNameMatch = false, isAnAgent = false,
          individual = None, organisation = Some(Organisation(limitedCompanyFormData.businessName, businessType)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def matchAndCache(matchBusinessData: MatchBusinessData, service: String)(implicit request: Request[AnyContent],
                                                                                    hc: HeaderCarrier): Future[Result] = {
    businessMatchingConnector.lookup(matchBusinessData) flatMap {
      actualResponse => {
        if (actualResponse.toString contains ("error")) {
          Future.successful(Redirect(controllers.routes.BusinessVerificationController.helloWorld(actualResponse.toString)))
        } else {
          dataCacheConnector.saveReviewDetails(actualResponse.as[ReviewDetails]) flatMap {
            cachedData =>
              Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
          }
        }
      }
    }
  }


}
