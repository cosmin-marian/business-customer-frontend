package controllers

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService

import scala.concurrent.Future

object HomeController extends HomeController {
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "HomeController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait HomeController extends BackLinkController {

  def businessMatchService: BusinessMatchingService

  def homePage(service: String, backLinkUrl: Option[String]) = AuthAction(service).async { implicit bcContext =>

    businessMatchService.matchBusinessWithUTR(isAnAgent = bcContext.user.isAgent, service) match {
      case Some(futureJsValue) =>
        futureJsValue flatMap {
          jsValue => jsValue.validate[ReviewDetails] match {
            case success: JsSuccess[ReviewDetails] => RedirectWithBackLink(ReviewDetailsController.controllerId, controllers.routes.ReviewDetailsController.businessDetails(service), backLinkUrl)
            case failure: JsError => RedirectWithBackLink(BusinessVerificationController.controllerId, controllers.routes.BusinessVerificationController.businessVerification(service), backLinkUrl)
          }
        }
      case None => RedirectWithBackLink(BusinessVerificationController.controllerId, controllers.routes.BusinessVerificationController.businessVerification(service), backLinkUrl)
    }
  }

}
