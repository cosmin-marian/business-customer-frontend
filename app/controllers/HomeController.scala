package controllers

import controllers.auth.BusinessCustomerRegime
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object HomeController extends HomeController{
  override val authConnector = FrontendAuthConnector
}

trait HomeController extends FrontendController with Actions {

  def homePage(service: String) = AuthorisedFor(BusinessCustomerRegime) {
    implicit user => implicit request =>
      user.userAuthority.accounts.sa.isDefined || user.userAuthority.accounts.ct.isDefined match {
        case true => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
        case false => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
      }
  }
}
