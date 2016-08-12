package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms._

object ClientPermissionController extends ClientPermissionController {
  val authConnector = FrontendAuthConnector
}

trait ClientPermissionController extends BaseController {

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    if (bcContext.user.isAgent) Ok(views.html.client_permission(clientPermissionForm, service))
    else Redirect(controllers.routes.BusinessRegController.register(service, "ATED"))
  }

  def continue(service: String) = AuthAction(service) { implicit bcContext =>
    clientPermissionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.client_permission(formWithErrors, service)),
      formData => {
        val permission = formData.permission.getOrElse(false)
        if (permission) Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP"))
        else Redirect("http://localhost:9916/ated/home")
      }
    )
  }

}
