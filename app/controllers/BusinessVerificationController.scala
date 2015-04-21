package controllers

import forms.BusinessVerificationForms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

object BusinessVerificationController extends BusinessVerificationController

trait BusinessVerificationController extends FrontendController {

   def show = Action { implicit request =>
     Ok(views.html.business_verification(businessDetailsForm))
   }

  def submit = Action {  implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => {println("~~~~~~~~~~~~" + formWithErrors.errors);BadRequest(views.html.business_verification(formWithErrors))},
      value => {println("~~~~~~~~~~~~" + value); Redirect(controllers.routes.BusinessVerificationController.helloWorld())}
    )
  }

  def helloWorld = Action {
    Ok(views.html.hello_world())
  }
}
