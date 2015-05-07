package controllers

import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

object HomeController extends HomeController

trait HomeController extends FrontendController {

  def homePage(service: String) = UnauthorisedAction { implicit request =>
    Ok("Success")
  }
}
