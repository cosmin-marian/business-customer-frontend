package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import services.{SubscriptionDetailsService, BusinessRegistrationService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object AgentController extends AgentController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  override val subscriptionDetailsService = SubscriptionDetailsService
}

trait AgentController extends FrontendController with Actions {

  val businessRegistrationService : BusinessRegistrationService
  val subscriptionDetailsService : SubscriptionDetailsService
  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      subscriptionDetailsService.fetchSubscriptionDetails.map{subscriptionDetails =>
        Ok
      }
  }
}
