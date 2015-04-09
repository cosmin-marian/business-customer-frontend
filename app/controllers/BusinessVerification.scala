package controllers

import controllers.common.BaseController
import forms.BusinessVerificationForms._
import play.api.mvc._

object BusinessVerification extends BusinessVerification

trait BusinessVerification extends BaseController{
   def show = Action { implicit request =>
     Ok(views.html.business_verification(businessDetailsForm))
   }

  def submit = Action {  implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.BusinessVerification.helloWorld()),
      value => Redirect(routes.BusinessVerification.helloWorld())
    )
  }

  def helloWorld = Action {
    Ok(views.html.hello_world())
  }
}
