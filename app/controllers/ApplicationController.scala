package controllers

import controllers.auth.ExternalUrls
import play.api.Play
import play.api.mvc.DiscardingCookie
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}


trait ApplicationController extends FrontendController with RunMode {

  import play.api.Play.current

  def unauthorised() = UnauthorisedAction {
    implicit request =>
      Ok(views.html.unauthorised(request))
  }

  def cancel = UnauthorisedAction { implicit request =>
    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.cancelRedirectUrl")
    Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
  }

  def logout = UnauthorisedAction { implicit request =>
    Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
  }

  def signedOut =  UnauthorisedAction { implicit request =>
    Ok(views.html.logout(request))
  }

  def logoutAndRedirectToHome(service:String) = UnauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }

  def redirectToAgentSummary(service: String) = UnauthorisedAction {
    implicit request =>
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.agentHomeUrl")
      Redirect(serviceRedirectUrl.getOrElse("/ated/home"))

  }

  def redirectToAgentLogout(service: String) = UnauthorisedAction {
    implicit request =>
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.agentLogoutUrl")
      Redirect(serviceRedirectUrl.getOrElse("/ated/logout"))

  }
}

object ApplicationController extends ApplicationController {}
