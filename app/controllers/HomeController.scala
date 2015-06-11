package controllers

import controllers.auth.BusinessCustomerRegime
import models.SubscriptionDetails
import services.{SubscriptionDetailsService, BusinessMatchingService}
import config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object HomeController extends HomeController {
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  val subscriptionDetailsService = SubscriptionDetailsService
  override val authConnector = FrontendAuthConnector
}

trait HomeController extends FrontendController with Actions {

  val businessMatchService: BusinessMatchingService
  val subscriptionDetailsService: SubscriptionDetailsService

  def homePage(service: String, isAgent: Boolean) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      subscriptionDetailsService.saveSubscriptionDetails(new SubscriptionDetails(service = service, isAgent = isAgent))
      user.principal.accounts.sa.isDefined || user.principal.accounts.ct.isDefined match {
        case true => {
          businessMatchService.matchBusiness flatMap {
            noException => {
              if(noException.toString().contains("error")){
                Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessVerification(service)))
              } else {
                Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
              }
            }
          }
        }
        case false => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessVerification(service)))
      }
  }
}
