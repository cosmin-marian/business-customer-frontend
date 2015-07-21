package controllers

import controllers.auth.ExternalUrls
import play.api.Play
import play.api.mvc.{DiscardingCookie, Action}
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}


trait ApplicationController extends FrontendController with RunMode {

  import play.api.Play.current

  def unauthorised() = Action {
    implicit request =>
      Ok(views.html.unauthorised(request))
  }

  def cancel = Action { implicit request =>
    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.cancelRedirectUrl")
    Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
  }

  def logout = Action { implicit request =>
    Redirect(ExternalUrls.signOut)
  }

  def logoutAndRedirectToHome(service:String) = UnauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }

}

object ApplicationController extends ApplicationController {}
