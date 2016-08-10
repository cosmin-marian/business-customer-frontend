package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerHelpers
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
          Redirect("http://localhost:9916/ated/home")
        } else {
          Redirect("http://localhost:9923/business-customer/next-page")
        }
    )
  }

}

object NonUkClientRegistrationController extends NonUkClientRegistrationController {
  val authConnector = FrontendAuthConnector
}
