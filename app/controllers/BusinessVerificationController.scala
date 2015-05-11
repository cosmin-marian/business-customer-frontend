package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessVerificationForms._
import forms._
import models.ReviewDetails
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
            case "SOP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "SOP")))
            case "UIB" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "UIB")))
            case "LTD" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LTD")))
            case "OBP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "OBP")))
            case "LLP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LLP")))
          }
        }
      )
  }

  def businessLookup(service: String, businessType: String) = Action { implicit request =>
    businessType match {
      case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, service))
      case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, service))
      case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, service))
      case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, service))
      case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, service))
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
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, service))),
      unincorporatedFormData => {
        val businessDetails = BusinessDetails(businessType, None, None, Some(unincorporatedFormData), None, None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, service))),
      soleTraderFormData => {
        val businessDetails = BusinessDetails(businessType, Some(soleTraderFormData), None, None, None, None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, service))),
      llpFormData => {
        val businessDetails = BusinessDetails(businessType, None, None, None, None, Some(llpFormData))
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, service))),
      obpFormData => {
        val businessDetails = BusinessDetails(businessType, None, None, None, Some(obpFormData), None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, service))),
      limitedCompanyFormData => {
        val businessDetails = BusinessDetails(businessType, None, Some(limitedCompanyFormData), None, None, None)
        matchAndCache(businessDetails, service)
      }
    )
  }

  private def matchAndCache(businessDetails: BusinessDetails, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
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
