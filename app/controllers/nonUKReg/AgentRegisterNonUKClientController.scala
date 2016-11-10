package controllers.nonUKReg

import config.FrontendAuthConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{ReviewDetails, BusinessRegistrationDisplayDetails}
import play.api.{Logger, Play}
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils

import scala.concurrent.Future

object AgentRegisterNonUKClientController extends AgentRegisterNonUKClientController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  // $COVERAGE-ON$
}

trait AgentRegisterNonUKClientController extends BaseController with RunMode {

  import play.api.Play.current

  def businessRegistrationService: BusinessRegistrationService

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
    Ok(views.html.nonUkReg.nonuk_business_registration(businessRegistrationForm, service, displayDetails, serviceRedirectUrl))
  }

  def edit(service: String, redirectUrl : Option[String]) = AuthAction(service).async { implicit bcContext =>

    val updateRedirectUrl = redirectUrl match {
      case Some(url) => Some(url)
      case None => Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
    }
    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.nonuk_business_registration(businessRegistrationForm.fill(detailsTuple._2), service, displayDetails, updateRedirectUrl, Some("edit")))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][edit] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }

  }

  def submit(service: String, mode : Option[String], redirectUrl : Option[String]) = AuthAction(service).async { implicit bcContext =>

    BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.nonuk_business_registration(formWithErrors, service, displayDetails, redirectUrl)))
      },
      registerData => {
        val saveResult = mode match {
          case Some("edit") => businessRegistrationService.updateRegisterBusiness(registerData, isGroup = false, isNonUKClientRegisteredByAgent = true, service)
          case _ => businessRegistrationService.registerBusiness(registerData, isGroup = false, isNonUKClientRegisteredByAgent = true, service)
        }

        saveResult.map { response =>
          redirectUrl match {
            case Some(serviceUrl) => Redirect(serviceUrl)
            case _ =>
              Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $service")
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
      BCUtils.getIsoCodeTupleList)
  }

}
