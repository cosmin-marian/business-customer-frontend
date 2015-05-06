package controllers

import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object HomeController extends HomeController {
  val authConnector = ???
}

trait HomeController extends FrontendController with Actions {

  val authConnector: AuthConnector

  def homePage(service: String) = AuthorisedFor(SaRegime) {  user => request =>
    Ok("Success")
  }
}
