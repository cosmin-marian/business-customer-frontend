package controllers

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import models.ReviewDetails
import config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val businessCustomerConnector = BusinessCustomerConnector
}

trait BusinessRegController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector
  val businessCustomerConnector: BusinessCustomerConnector

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service))
  }

  def back(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
  }


  def send(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessRegistrationForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service)))
        },
        registrationData => {
          businessCustomerConnector.register(registrationData).flatMap {
            registrationSuccessResponse => {
              dataCacheConnector.saveReviewDetails(registrationSuccessResponse.as[ReviewDetails]) flatMap {
                cachedData =>
                  Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
              }
            }
          }
        }
      )
  }

}
