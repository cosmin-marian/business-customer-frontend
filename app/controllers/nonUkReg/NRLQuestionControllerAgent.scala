package controllers.nonUkReg

import config.FrontendAuthConnector
import controllers.BaseController
import controllers.auth.ExternalUrls
import forms.BusinessRegistrationForms._

trait NRLQuestionControllerAgent extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.nonukreg.nrl_question_agent(nrlQuestionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    nrlQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.nonukreg.nrl_question_agent(formWithErrors, service)),
      formData => {
        if (formData.paysSA.contains(true)) Redirect(ExternalUrls.serviceAccountPath("ATED"))
        else Redirect(controllers.nonUkReg.routes.ClientPermissionController.view("ATED"))
      }
    )
  }

  def back(service: String) = AuthAction(service) { implicit bcContext =>
    Redirect(ExternalUrls.addClientEmailPath)
  }

}

object NRLQuestionControllerAgent extends NRLQuestionControllerAgent {
  val authConnector = FrontendAuthConnector
}
