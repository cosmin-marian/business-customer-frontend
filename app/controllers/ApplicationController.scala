package controllers

import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ApplicationController extends FrontendController {

  def unauthorised() = Action {
    implicit request =>
      Ok(views.html.unauthorised(request))
  }

}
