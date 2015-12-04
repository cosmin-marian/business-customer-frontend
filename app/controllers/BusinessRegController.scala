package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails, Address}
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{BCUtils, AuthUtils}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import play.api.i18n.Messages
import play.api.data.Form

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends BaseController {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service, displayDetails(businessType)))
  }

  def send(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service, displayDetails(businessType))))
        },
        registrationData => {
              businessRegistrationService.registerBusiness(registrationData, false).map {
                registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, false))
              }
        }
      )
  }

  private def displayDetails(businessType: String)(implicit user: AuthContext) = {
    if (AuthUtils.isAgent) {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.agent.non-uk.header"),
        Messages("bc.business-registration.business.subheader"),
        BCUtils.getIsoCodeTupleList)
    } else {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.business.subheader"),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

