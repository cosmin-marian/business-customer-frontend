package controllers.nonUkReg

import config.FrontendAuthConnector
import controllers.BaseController
import controllers.auth.ExternalUrls
import forms.BusinessRegistrationForms._



trait AtedOneQuestionController extends BaseController {



  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.atedone_question(atedOneQuestionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "ATED"))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    atedOneQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.atedone_question(formWithErrors, service)),
      formData => {
        val ated1 = formData.ated1.getOrElse(false)
        if (ated1) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
        else Redirect(ExternalUrls.serviceAccountPath("ATED"))
      }
    )
  }

}

object AtedOneQuestionController extends AtedOneQuestionController {
  val authConnector = FrontendAuthConnector

}