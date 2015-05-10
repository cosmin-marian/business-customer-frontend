package controllers

import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import forms.BusinessRegistrationForms._



object BusinessRegController extends BusinessRegController

trait BusinessRegController extends FrontendController {


  def register = UnauthorisedAction { implicit request =>
    Ok(views.html.business_registration(businessRegistrationForm))
  }

  def send(service: String) = UnauthorisedAction { implicit request =>
    businessRegistrationForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.business_registration(formWithErrors))
      },
      value => {
        Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
      }

    )
  }

}

