package controllers

import controllers.auth.BusinessCustomerRegime
import models.ReviewDetails
import services.BusinessMatchingService
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.frontend.auth.User

import scala.concurrent.Future

object HomeController extends HomeController{
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  override val authConnector = FrontendAuthConnector
}

trait HomeController extends FrontendController with Actions {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      user.userAuthority.accounts.sa.isDefined || user.userAuthority.accounts.ct.isDefined match {
        case true => {
          matchBusiness(service)
          Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
        }
        case false => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
      }
  }

  private final def matchBusiness(service: String)(implicit request: Request[AnyRef], user: User) = {
    businessMatchService.matchBusiness map {data => data} recover {
      case error =>
        println("ooooooooooooooooooooooooooooooooooooooooooooooooo" + error)
        Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
    }
  }
}
