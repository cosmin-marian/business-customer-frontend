package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.BusinessRegistrationDisplayDetails
import play.api.Logger
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthUtils, BCUtils}

import scala.concurrent.Future

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait BusinessRegController extends BaseController {

  val businessRegistrationService: BusinessRegistrationService

  val countryUnitedKingdom = "GB"

  def register(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service, displayDetails(businessType, service)))
  }

  def send(service: String, businessType: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service, displayDetails(businessType, service))))
        },
        registrationData => {
          val businessRegCountry = registrationData.businessAddress.country
          businessRegCountry match {
            case `countryUnitedKingdom` => {
              val errorMsg = Messages("bc.business-verification-error.non-uk")
              val errorForm = businessRegistrationForm.withError(key = "businessAddress.country", message = errorMsg).fill(registrationData)
              Future.successful(BadRequest(views.html.business_registration(errorForm, service, displayDetails(businessType, service))))
            }
            case _ => {
              businessRegistrationService.registerBusiness(registrationData, isGroup = false).map {
                registrationSuccessResponse => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service, false))
              }
            }
          }
        }
      )
  }

  private def displayDetails(businessType: String, service: String)(implicit user: AuthContext) = {
    if (AuthUtils.isAgent) {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.agent.non-uk.header"),
        Messages("bc.business-registration.text.agent", service),
        BCUtils.getIsoCodeTupleList)
    } else {
      new BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        BCUtils.getIsoCodeTupleList)
    }
  }
}

