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

object BusinessRegGroupGBController extends BusinessRegGroupGBController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegGroupGBController extends BaseController {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      val newMapping = businessRegistrationForm.data + ("businessAddress.country" -> "GB")
      Ok(views.html.business_group_registration(businessRegistrationForm.copy(data = newMapping), service, displayDetails))
  }

  def send(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      BusinessRegistrationForms.validateForm(businessRegistrationForm.bindFromRequest, true).fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_group_registration(formWithErrors, service, displayDetails)))
        },
        registrationData => {
          businessRegistrationService.registerBusiness(registrationData, true).map {
            registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, false))
          }
        }
      )
  }

  private def displayDetails()(implicit user: AuthContext) = {
    new BusinessRegistrationDisplayDetails("GROUP",
      Messages("bc.business-registration.user.group.header"),
      Messages("bc.business-registration.group.subheader"),
      BCUtils.getIsoCodeTupleList)
  }
}

