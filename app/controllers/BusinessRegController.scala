package controllers

import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import forms.BusinessRegistrationForms._



object BusinessRegController extends BusinessRegController

trait BusinessRegController extends FrontendController {


  def register = UnauthorisedAction { implicit request =>
    Ok(views.html.business_registration(businessRegistrationForm))
  }

}

