package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms.nrlQuestionForm
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object NRLQuestionController extends NRLQuestionController {
  val authConnector = FrontendAuthConnector
}

trait NRLQuestionController extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
    else Ok(views.html.nrl_question(nrlQuestionForm, service))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    nrlQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.nrl_question(formWithErrors, service)),
      formData => {
        val paysSa = formData.paysSA.getOrElse(false)
        if (paysSa) Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
        else Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
      }
    )
  }

}
