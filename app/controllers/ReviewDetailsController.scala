package controllers

import forms.BusinessVerificationForms._
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController


object ReviewDetailsController extends ReviewDetailsController {

}

trait ReviewDetailsController extends FrontendController {

  def helloWorld = Action {
    Ok(views.html.hello_world())
  }

  def subscribe = Action {
    Ok(views.html.subscription())
  }

  def details = Action { implicit request =>
    Ok(views.html.review_details())
  }

}