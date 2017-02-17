package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{BackLinkController, BaseController}
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
  override val controllerId: String = "BusinessRegController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait BusinessRegController extends BackLinkController {

  def businessRegistrationCache: BusinessRegCacheConnector

  def register(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    currentBackLink.map(backLink =>
        Ok(views.html.nonUkReg.business_registration(businessRegistrationForm, service, displayDetails(businessType, service), backLink))
    )
  }


  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.business_registration(formWithErrors, service, displayDetails(businessType, service), backLink))
        )
      },
      registrationData => {
        businessRegistrationCache.saveBusinessRegDetails(registrationData).map {
          registrationSuccessResponse => Redirect(controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, false))
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

