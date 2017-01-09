package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BusinessRegCacheConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{OverseasCompany, BusinessCustomerContext, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import services.BusinessRegistrationService
import utils.BCUtils

import scala.concurrent.Future

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
}

trait BusinessRegController extends BaseController {

  def businessRegistrationCache: BusinessRegCacheConnector

  def register(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
    Ok(views.html.nonUkReg.business_registration(businessRegistrationForm, service, displayDetails(businessType, service)))
  }


  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.business_registration(formWithErrors, service, displayDetails(businessType, service))))
      },
      registrationData => {
        businessRegistrationCache.saveBusinessRegDetails(registrationData).map {
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
        None,
        BCUtils.getIsoCodeTupleList)
    } else {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        Some(Messages("bc.business-registration.lede.text")),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

