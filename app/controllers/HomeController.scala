package controllers

import controllers.auth.BusinessCustomerRegime
import services.BusinessMatchingService
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object HomeController extends HomeController{
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  override val authConnector = FrontendAuthConnector
}

trait HomeController extends FrontendController with Actions {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      user.userAuthority.accounts.sa.isDefined || user.userAuthority.accounts.ct.isDefined match {
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
