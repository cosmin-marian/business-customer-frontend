package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerHelpers
import forms.BusinessRegistrationForms._

/**
 * Created by sinead on 09/08/16.
 */
trait NonUkClientRegistration extends BaseController {

  def uniqueTaxReferenceView= AuthAction("ATED") { implicit bcContext =>
    Ok (views.html.nonuk_client_registration(areYouAnAgentForm))

  }

}

object NonUkClientRegistration extends NonUkClientRegistration {
  val authConnector = FrontendAuthConnector
}
