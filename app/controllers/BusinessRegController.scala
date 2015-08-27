package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{BCUtils, AuthUtils}

import scala.concurrent.Future
import play.api.i18n.Messages


object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends FrontendController with Actions {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, AuthUtils.isAgent, service, BCUtils.getIsoCodeTupleList))
  }

  def back(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
  }


  def send(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessRegistrationForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, AuthUtils.isAgent, service, BCUtils.getIsoCodeTupleList)))
        },
        registrationData => {
          (registrationData.businessUniqueId, registrationData.issuingInstitution) match {
            case (Some(id), None) => {
              val errorMsg = Messages("bc.business-registration-error.issuingInstitution.select")
              val errorForm = businessRegistrationForm.withError(key = "issuingInstitution", message = errorMsg).fill(registrationData)
              Future.successful(BadRequest(views.html.business_registration(errorForm, AuthUtils.isAgent, service, BCUtils.getIsoCodeTupleList)))
            }
            case(None, Some(inst)) => {
              val errorMsg = Messages("bc.business-registration-error.businessUniqueId.select")
              val errorForm = businessRegistrationForm.withError(key = "businessUniqueId", message = errorMsg).fill(registrationData)
              Future.successful(BadRequest(views.html.business_registration(errorForm, AuthUtils.isAgent, service, BCUtils.getIsoCodeTupleList)))
            }
            case _ => {
              businessRegistrationService.registerBusiness(registrationData).map {
                registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
            }
          }
        }
      )
  }

}
