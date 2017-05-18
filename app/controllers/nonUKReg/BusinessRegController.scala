package controllers.nonUKReg

import config.{ApplicationConfig, FrontendAuthConnector}
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{BackLinkController, BaseController}
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistration, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import utils.BCUtils
import utils.BusinessCustomerConstants.businessRegDetailsId

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
    for {
      backLink <- currentBackLink
      businessRegistration <- businessRegistrationCache.fetchAndGetBusinessRegForSession[BusinessRegistration](businessRegDetailsId)
    } yield {
      businessRegistration match {
        case Some(businessReg) =>
          Ok(views.html.nonUkReg.business_registration(businessRegistrationForm.fill(businessReg), service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
        case None =>
          Ok(views.html.nonUkReg.business_registration(businessRegistrationForm, service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
      }
    }
  }

  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, bcContext.user.isAgent).fold(
      formWithErrors => {
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.business_registration(formWithErrors, service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
        )
      },
      registrationData => {
        businessRegistrationCache.saveBusinessRegDetails[BusinessRegistration](businessRegDetailsId,registrationData).flatMap {
          registrationSuccessResponse =>
            RedirectWithBackLink(
              OverseasCompanyRegController.controllerId,
              controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, false),
              Some(controllers.nonUKReg.routes.BusinessRegController.register(service, businessType).url)
            )
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

