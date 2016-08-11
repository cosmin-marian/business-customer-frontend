package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms.nrlQuestionForm

object NRLQuestionControllerAgent extends NRLQuestionControllerAgent {
  val authConnector = FrontendAuthConnector
}

trait NRLQuestionControllerAgent extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.nrl_question_agent(nrlQuestionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "ATED"))

  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    nrlQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.nrl_question_agent(formWithErrors, service)),
      formData => {
        val paysSa = formData.paysSA.getOrElse(false)
        if (paysSa) Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
        else Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
      }
    )
  }

}
