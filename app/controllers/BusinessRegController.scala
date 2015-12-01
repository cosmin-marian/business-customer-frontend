package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails, Address}
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{BCUtils, AuthUtils}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import play.api.i18n.Messages
import play.api.data.Form

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends BaseController {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(viewsBusinessRegForm(businessRegistrationForm.copy(data = Map("businessType" -> businessType)), service, businessType))
  }


  def back(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Redirect(controllers.routes.BusinessVerificationController.businessVerification(service))
  }


  def send(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      validateForm(businessRegistrationForm.bindFromRequest, businessType).fold(
        formWithErrors => {
          Future.successful(BadRequest(viewsBusinessRegForm(formWithErrors, service, businessType)))
        },
        registrationData => {
              businessRegistrationService.registerBusiness(registrationData, isGroup(businessType)).map {
                registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, false))
              }
        }
      )
  }

  private def validateForm(registrationData: Form[BusinessRegistration], businessType: String) = {
    validateInstitution(registrationData, businessType)
  }

  private def validateInstitution(registrationData: Form[BusinessRegistration], businessType: String) = {
    val businessUniqueId = registrationData.data.get("businessUniqueId")
    val issuingInstitution = registrationData.data.get("issuingInstitution")
    (businessUniqueId, issuingInstitution) match {
      case (Some(id), None) => registrationData.withError(key = "issuingInstitution",
        message = Messages("bc.business-registration-error.issuingInstitution.select"))
      case(None, Some(inst)) => registrationData.withError(key = "businessUniqueId",
        message = Messages("bc.business-registration-error.businessUniqueId.select"))
      case _ => registrationData
    }
  }

  private def viewsBusinessRegForm(formToView: Form[BusinessRegistration], service: String, businessType: String)
                                  (implicit user: AuthContext, request: play.api.mvc.Request[_]) = {
    isGroup(businessType) match {
      case true => {
        val newMapping = formToView.data + ("businessAddress.country" -> "GB")
        views.html.business_group_registration(formToView.copy(data = newMapping),
          service, displayDetails(businessType))
      }
      case false => views.html.business_registration(formToView, service, displayDetails(businessType))
    }
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

