package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms
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

object BusinessRegUKController extends BusinessRegUKController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegUKController extends BaseController {

  val businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      val newMapping = businessRegistrationForm.data + ("businessAddress.country" -> "GB")
      Ok(views.html.business_group_registration(businessRegistrationForm.copy(data = newMapping), service, displayDetails(businessType)))
  }

  def send(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      BusinessRegistrationForms.validateUK(businessRegistrationForm.bindFromRequest).fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_group_registration(formWithErrors, service, displayDetails(businessType))))
        },
        registrationData => {
          businessRegistrationService.registerBusiness(registrationData, isGroup(businessType)).map {
            registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, false))
          }
        }
      )
  }

  private def isGroup(businessType: String) = {
    businessType.equals("GROUP")
  }

  private def displayDetails(businessType: String)(implicit user: AuthContext) = {
    if (isGroup(businessType)) {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.group.header"),
        Messages("bc.business-registration.group.subheader"),
        BCUtils.getIsoCodeTupleList)
    }
    else {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.new-business.header"),
        Messages("bc.business-registration.business.subheader"),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

