package controllers


import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import forms.BusinessRegistrationForms._



object BusinessRegController extends BusinessRegController{

}

trait BusinessRegController extends FrontendController {



  def register = Action { implicit request =>
    Ok(views.html.business_registration(businessRegistrationForm))
  }

}