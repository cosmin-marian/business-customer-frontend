package controllers.nonUkReg

import config.FrontendAuthConnector
import controllers.BaseController
import controllers.auth.ExternalUrls
import forms.BusinessRegistrationForms._


trait ClientPermissionController extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.nonukreg.client_permission(clientPermissionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "ATED"))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    clientPermissionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.nonukreg.client_permission(formWithErrors, service)),
      formData => {
        if (formData.permission.contains(true)) Redirect(controllers.routes.BusinessRegController.register(service, "NUK"))
        else Redirect(ExternalUrls.serviceAccountPath("ATED"))
      }
    )
  }

}

object ClientPermissionController extends ClientPermissionController {
  val authConnector = FrontendAuthConnector
}
