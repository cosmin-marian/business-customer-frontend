package controllers

import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.config.RunMode
import play.api.Play


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

}

object ApplicationController extends ApplicationController {}
