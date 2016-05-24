package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms.nrlQuestionForm

object NRLQuestionController extends NRLQuestionController {
  override val authConnector = FrontendAuthConnector
}

trait NRLQuestionController extends BaseController {

  def view(service: String) = AuthAction(service) {
    implicit bcContext =>
      if (bcContext.user.isAgent) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
      else Ok(views.html.nrl_question(nrlQuestionForm, service))
  }

  def continue(service: String) = AuthAction(service) {
    implicit bcContext =>
      nrlQuestionForm.bindFromRequest.fold(
        fornWithErrors => BadRequest(views.html.nrl_question(fornWithErrors, service)),
        formData => {
          val paysSa = formData.paysSA.getOrElse(false)
          if (paysSa) Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
          else Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
        }
      )
  }

}
