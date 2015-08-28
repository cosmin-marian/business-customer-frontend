package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import models.BusinessRegistrationDisplayDetails
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{BCUtils, AuthUtils}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import play.api.i18n.Messages

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends FrontendController with Actions {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service, displayDetails(businessType)))
  }


  def back(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
  }


  def send(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessRegistrationForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service, displayDetails(businessType))))
        },
        registrationData => {
          (registrationData.businessUniqueId, registrationData.issuingInstitution) match {
            case (Some(id), None) => {
              val errorMsg = Messages("bc.business-registration-error.issuingInstitution.select")
              val errorForm = businessRegistrationForm.withError(key = "issuingInstitution", message = errorMsg).fill(registrationData)
              Future.successful(BadRequest(views.html.business_registration(errorForm, service, displayDetails(businessType))))
            }
            case(None, Some(inst)) => {
              val errorMsg = Messages("bc.business-registration-error.businessUniqueId.select")
              val errorForm = businessRegistrationForm.withError(key = "businessUniqueId", message = errorMsg).fill(registrationData)
              Future.successful(BadRequest(views.html.business_registration(errorForm, service, displayDetails(businessType))))
            }
            case _ => {
              businessRegistrationService.registerBusiness(registrationData, isGroup(businessType)).map {
                registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
              }
            }
          }
        }
      )
  }

  private def isGroup(businessType: String) = {
    businessType.equals("GROUP")
  }

  private def displayDetails(businessType: String)(implicit user: AuthContext) = {
    businessType match {
      case "NUK" => {
        if (AuthUtils.isAgent) {
          new BusinessRegistrationDisplayDetails(businessType,
            Messages("bc.business-registration.agent.non-uk.header"),
            Messages("bc.business-registration.business.subheader"),
            BCUtils.getIsoCodeTupleList)
        } else {
          new BusinessRegistrationDisplayDetails(businessType,
            Messages("bc.business-registration.user.non-uk.header"),
            Messages("bc.business-registration.business.subheader"),
            BCUtils.getIsoCodeTupleList)
        }
      }
      case "GROUP" =>  new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.group.header"),
        Messages("bc.business-registration.group.subheader"),
        BCUtils.getIsoCodeTupleList)
      case _ => new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.new-business.header"),
        Messages("bc.business-registration.business.subheader"),
        BCUtils.getIsoCodeTupleList)

    }
  }
}

