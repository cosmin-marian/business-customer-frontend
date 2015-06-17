package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object AgentController extends AgentController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait AgentController extends FrontendController with Actions {

  val businessRegistrationService : BusinessRegistrationService

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
        Ok(views.html.business_agent_confirmation(request))
  }
}
