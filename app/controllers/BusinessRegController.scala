package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends FrontendController with Actions {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service))
  }

  def back(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
  }


  def send(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessRegistrationForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service)))
        },
        registrationData => {
          businessRegistrationService.registerNonUk(registrationData).map {
            registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
          }
        }
      )
  }

}
