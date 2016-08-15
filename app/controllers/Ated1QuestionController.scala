package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms._

object Ated1QuestionController extends Ated1QuestionController {
  val authConnector = FrontendAuthConnector
}

trait Ated1QuestionController extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.ated1_question(ated1QuestionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "ATED"))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    ated1QuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.ated1_question(formWithErrors, service)),
      formData => {
        val ated1 = formData.ated1.getOrElse(false)
        if (ated1) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
        else Redirect("http://localhost:9916/ated/home")
      }
    )
  }

}
