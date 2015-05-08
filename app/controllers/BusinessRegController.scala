package controllers

import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
}

trait BusinessRegController extends FrontendController with Actions {

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service))
  }

}

