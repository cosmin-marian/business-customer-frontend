package controllers

import config.FrontendAuthConnector
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService

import scala.concurrent.Future

trait HomeController extends BaseController {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthAction(service).async {
    implicit businessCustomerUser =>
      businessMatchService.matchBusinessWithUTR(isAnAgent = businessCustomerUser.user.isAgent, service) match {
        case Some(futureJsValue) =>
          futureJsValue map {
            jsValue => jsValue.validate[ReviewDetails] match {
              case success: JsSuccess[ReviewDetails] => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              case failure: JsError => Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
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
