package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait HomeController extends FrontendController with Actions {

  val businessMatchService: BusinessMatchingService

  def homePage(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessMatchService.matchBusinessWithUTR(isAnAgent = false) match {
        case Some(futureJsValue) => {
          futureJsValue map {
            jsValue => jsValue.validate[ReviewDetails] match {
              case success: JsSuccess[ReviewDetails] => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
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
