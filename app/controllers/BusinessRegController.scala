package controllers

import config.FrontendAuthConnector
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages
import services.BusinessRegistrationService
import utils.BCUtils

import scala.concurrent.Future

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends BaseController {

  def businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
    Ok(views.html.business_registration(businessRegistrationForm, service, displayDetails(businessType, service)))
  }

  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.business_registration(formWithErrors, service, displayDetails(businessType, service))))
      },
      registrationData => {
        businessRegistrationService.registerBusiness(registrationData, isGroup = false, isNonUKClientRegisteredByAgent = false, service).map {
          registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
        }
      }
    )
  }

  private def displayDetails(businessType: String, service: String)(implicit bcContext: BusinessCustomerContext) = {
    if (bcContext.user.isAgent) {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.agent.non-uk.header"),
        Messages("bc.business-registration.text.agent", service),
        BCUtils.getIsoCodeTupleList)
    } else {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

