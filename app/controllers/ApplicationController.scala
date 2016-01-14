package controllers

import play.api.Play
import play.api.mvc.DiscardingCookie
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import models.FeedbackForm.feedbackForm


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

  def logout(service: String) = UnauthorisedAction {
    implicit request =>
      service.toUpperCase match {
        case "ATED" => Redirect(controllers.routes.ApplicationController.feedback(service)).withNewSession
        case _ => Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
      }
  }

  def feedback(service: String) = UnauthorisedAction {
    implicit request =>
      service.toUpperCase match {
        case "ATED" => Ok(views.html.feedback(feedbackForm, service))
        case _ => Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
      }
  }

  def signedOut = UnauthorisedAction { implicit request =>
    Ok(views.html.logout(request))
  }

  def logoutAndRedirectToHome(service: String) = UnauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }
}

object ApplicationController extends ApplicationController {}
