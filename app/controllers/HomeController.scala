package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object HomeController extends HomeController {
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  override val authConnector = FrontendAuthConnector
}

trait HomeController extends FrontendController with Actions {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      user.principal.accounts.sa.isDefined || user.principal.accounts.ct.isDefined match {
        case true => {
          businessMatchService.matchBusinessWithUTR(false) map {
            futureJsValue => futureJsValue map {
              jsValue => jsValue.validate[ReviewDetails] match {
                case success: JsSuccess[ReviewDetails] => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
                case failure: JsError => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
              }
            }
          }
        }
        case false => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
      }
  }
}
