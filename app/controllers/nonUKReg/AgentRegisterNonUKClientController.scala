package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BusinessRegCacheConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{OverseasCompany, BusinessRegistrationDisplayDetails}
import play.api.{Logger, Play}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils

import scala.concurrent.Future

object AgentRegisterNonUKClientController extends AgentRegisterNonUKClientController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
  // $COVERAGE-ON$
}

trait AgentRegisterNonUKClientController extends BaseController with RunMode {

  import play.api.Play.current

  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    Ok(views.html.nonUkReg.nonuk_business_registration(businessRegistrationForm, service, displayDetails))
  }

  def submit(service: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.nonuk_business_registration(formWithErrors, service, displayDetails)))
      },
      registerData => {
        businessRegistrationCache.saveBusinessRegDetails(registerData).map {
          registrationSuccessResponse =>
            val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
            serviceRedirectUrl match {
              case Some(redirectUrl) =>
                Redirect(controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, true, serviceRedirectUrl))
              case _ =>
                Logger.warn(s"[ReviewDetailsController][submit] - No Service config found for = $service")
                throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
            }
        }
      }
    )
  }

  private def displayDetails = {
    BusinessRegistrationDisplayDetails("NUK",
      Messages("bc.non-uk-reg.header"),
      Messages("bc.non-uk-reg.sub-header"),
      Some(Messages("bc.non-uk-reg.lede.text")),
      BCUtils.getIsoCodeTupleList)
  }

}