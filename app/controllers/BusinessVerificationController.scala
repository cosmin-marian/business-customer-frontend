package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessVerificationForms._
import forms._
import models.{BusinessMatchDetails, Individual, Organisation, ReviewDetails}
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

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
      }
  }

  def submit(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessType match {
        case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service)
        case "SOP" => sopFormHandling(soleTraderForm, businessType, service)
        case "LLP" => llpFormHandling(limitedLiabilityPartnershipForm, businessType, service)
        case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service)
        case "LTD" => ltdFormHandling(limitedCompanyForm, businessType, service)
      }
  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, service, businessType))),
      unincorporatedFormData => {
        val businessDetails = BusinessMatchDetails(false, "", None, Some(Organisation(unincorporatedFormData.businessName, unincorporatedFormData.cotaxUTR)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, service, businessType))),
      soleTraderFormData => {
        val businessDetails = BusinessMatchDetails(false, "", Some(Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, "", soleTraderFormData.saUTR)), None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, service, businessType))),
      llpFormData => {
        val businessDetails = BusinessMatchDetails(false, "", None, Some(Organisation(llpFormData.businessName, llpFormData.psaUTR)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, service, businessType))),
      obpFormData => {
        val businessDetails = BusinessMatchDetails(false, "", None, Some(Organisation(obpFormData.businessName, obpFormData.psaUTR)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, service, businessType))),
      limitedCompanyFormData => {
        val businessDetails = BusinessMatchDetails(false, "", None, Some(Organisation(limitedCompanyFormData.businessName, limitedCompanyFormData.cotaxUTR)))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def matchAndCache(businessDetails: BusinessMatchDetails, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    businessMatchingConnector.lookup(businessDetails) flatMap {
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
