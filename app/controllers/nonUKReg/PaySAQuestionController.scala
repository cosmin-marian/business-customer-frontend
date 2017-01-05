package controllers.nonUKReg

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms.paySAQuestionForm
import controllers.BaseController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object PaySAQuestionController extends PaySAQuestionController {
  val authConnector = FrontendAuthConnector
}

trait PaySAQuestionController extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
    else Ok(views.html.nonUkReg.paySAQuestion(paySAQuestionForm, service))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    paySAQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.nonUkReg.paySAQuestion(formWithErrors, service)),
      formData => {
        val paysSa = formData.paySA.getOrElse(false)
        if (paysSa) Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
        else Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
      }
    )
  }

}
