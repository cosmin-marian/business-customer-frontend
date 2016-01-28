package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService
import utils.AuthUtils

import scala.concurrent.Future

trait HomeController extends BaseController {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessMatchService.matchBusinessWithUTR(isAnAgent = AuthUtils.isAgent, service) match {
        case Some(futureJsValue) => {
          futureJsValue map {
            jsValue => jsValue.validate[ReviewDetails] match {
              case success: JsSuccess[ReviewDetails] => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, true))
              case failure: JsError => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
            }
          }
        }
        case None => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessVerification(service)))
      }
  }

}

object HomeController extends HomeController {
  override val businessMatchService: BusinessMatchingService = BusinessMatchingService
  override val authConnector = FrontendAuthConnector
}
