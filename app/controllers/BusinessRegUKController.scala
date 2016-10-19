package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.BusinessRegistrationDisplayDetails
import play.api.i18n.Messages
import services.BusinessRegistrationService
import utils.BCUtils

import scala.concurrent.Future

object BusinessRegUKController extends BusinessRegUKController {
  val authConnector = FrontendAuthConnector
  val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegUKController extends BaseController {

  def businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
    val newMapping = businessRegistrationForm.data + ("businessAddress.country" -> "GB") + ("hasBusinessUniqueId" -> "false")
    Ok(views.html.business_group_registration(businessRegistrationForm.copy(data = newMapping), service, displayDetails(businessType)))
  }

  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.business_group_registration(formWithErrors, service, displayDetails(businessType))))
      },
      registrationData => {
        businessRegistrationService.registerBusiness(registrationData, isGroup(businessType), isNonUKClientRegisteredByAgent = false, service).map {
          registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
        }
      }
    )
  }

  private def isGroup(businessType: String) = {
    businessType.equals("GROUP")
  }

  private def displayDetails(businessType: String) = {
    if (isGroup(businessType)) {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.group.header"),
        Messages("bc.business-registration.group.subheader"),
        BCUtils.getIsoCodeTupleList)
    }
    else {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.new-business.header"),
        Messages("bc.business-registration.business.subheader"),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

