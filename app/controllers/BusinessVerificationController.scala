package controllers

import forms.BusinessVerificationForms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import connectors.BusinessCustomerConnector
import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
}

trait BusinessVerificationController extends FrontendController {

  val businessCustomerConnector: BusinessCustomerConnector

   def show = Action { implicit request =>
     Ok(views.html.business_verification(businessDetailsForm))
   }

  def submit = Action.async {  implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors))),
      value => {
        businessCustomerConnector.lookup(value) map {
          actualResponse => Redirect(controllers.routes.BusinessVerificationController.helloWorld(actualResponse.toString()))
        }
      }
    )
  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }
}
