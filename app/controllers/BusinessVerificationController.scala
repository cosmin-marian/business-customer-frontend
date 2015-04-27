package controllers

import java.util.UUID

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import forms.BusinessVerificationForms._
import models.ReviewDetails
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  val businessCustomerConnector: BusinessCustomerConnector = BusinessCustomerConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessVerificationController extends FrontendController {

  val businessCustomerConnector: BusinessCustomerConnector
  val dataCacheConnector: DataCacheConnector

   def businessVerification(service: String) = Action { implicit request =>
     Ok(views.html.business_verification(businessDetailsForm, service)).withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
   }

  def submit(service: String) = Action.async {  implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors, service))),
      value => {
        businessCustomerConnector.lookup(value) map {
          actualResponse => {
            if(actualResponse.toString() contains("error")){
              Redirect(controllers.routes.BusinessVerificationController.helloWorld(actualResponse.toString()))
            }else{
              dataCacheConnector.saveBusinessDetails(actualResponse.as[ReviewDetails])
              Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
            }
          }
        }
      }
    )
  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }
}
