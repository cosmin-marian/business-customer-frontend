package controllers

import config.FrontendAuthConnector
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService

import scala.concurrent.Future

object HomeController extends HomeController {
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  val authConnector = FrontendAuthConnector
}

trait HomeController extends BaseController {

  def businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthAction(service).async { implicit bcContext =>
    businessMatchService.matchBusinessWithUTR(isAnAgent = bcContext.user.isAgent, service) match {
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
