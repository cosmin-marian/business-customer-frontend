package controllers.nonUkReg

import config.FrontendAuthConnector
import controllers.BaseController
import controllers.auth.ExternalUrls
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._


trait NonUkClientRegistrationController extends BaseController {

  def uniqueTaxReferenceView = AuthAction("ATED") { implicit bcContext =>
    Ok(views.html.nonuk_client_registration(notUkUtrForm))
  }

  def uniqueTaxReferenceSubmit = AuthAction("ATED") { implicit bcContext =>
    BusinessRegistrationForms.notUkUtrForm.bindFromRequest.fold(
      formWithError => BadRequest(views.html.nonuk_client_registration(formWithError)),
      data =>
        if (data.nUkUtr.contains(true)) {
          Redirect(ExternalUrls.serviceAccountPath("ATED"))
        } else {
          Redirect(controllers.nonUkReg.routes.NRLQuestionControllerAgent.view("ATED"))
        }
    )
  }

}

object NonUkClientRegistrationController extends NonUkClientRegistrationController {
  val authConnector = FrontendAuthConnector
}