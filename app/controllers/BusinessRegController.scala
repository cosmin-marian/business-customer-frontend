package controllers

import controllers.auth.BusinessCustomerRegime
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import forms.BusinessRegistrationForms._

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
}

trait BusinessRegController extends FrontendController  with Actions {

  def register = AuthorisedFor(BusinessCustomerRegime) { implicit user => implicit request =>
    Ok(views.html.business_registration(businessRegistrationForm))
  }

}

