package controllers

import forms.BusinessVerificationForms._
import models.ReviewDetails
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController


object ReviewDetailsController extends ReviewDetailsController {

}

trait ReviewDetailsController extends FrontendController {

  def subscribe = Action {
    Ok(views.html.subscription())
  }

  def details = Action { implicit request =>
    Ok(views.html.review_details(ReviewDetails("ACME", "Limited", "23 High Street Park View The Park Gloucester Gloucestershire ABC 123","01234567890", "contact@acme.com")))
  }


}