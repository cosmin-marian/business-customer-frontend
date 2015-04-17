package controllers

import forms.BusinessVerificationForms._
import forms.ReviewDetailsForms
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
    Ok(views.html.review_details(ReviewDetailsForms))
  }


  def subscription = Action {  implicit request =>
    ReviewDetailsForms.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.business_verification(formWithErrors)),
      value => Redirect(controllers.routes.BusinessVerificationController.subscribe)
    )
  }
}