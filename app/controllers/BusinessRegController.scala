package controllers


import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import forms.BusinessVerificationForms._
import uk.gov.hmrc.play.http.SessionKeys
import java.util.UUID


object BusinessRegController extends BusinessRegController{

}

trait BusinessRegController extends FrontendController {

  def register(response: String) = Action {
    Ok("Yes")
  }

  /*

  def businessRegistration = Action { implicit request =>
      Ok(views.html.business_registration)
    }*/
}