package controllers.nonUKReg

import config.FrontendAuthConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistrationDisplayDetails}
import play.api.Logger
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils

import scala.concurrent.Future

object UpdateNonUKBusinessRegistrationController extends UpdateNonUKBusinessRegistrationController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  // $COVERAGE-ON$
}

trait UpdateNonUKBusinessRegistrationController extends BaseController with RunMode {

  def businessRegistrationService: BusinessRegistrationService

  def editAgent(service: String) = AuthAction(service).async { implicit bcContext =>
    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.update_business_registration(businessRegistrationForm.fill(detailsTuple._2), service, displayDetails(service, false), None, false))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][editAgent] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }
  }

  def edit(service: String, redirectUrl : Option[String]) = AuthAction(service).async { implicit bcContext =>
    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.update_business_registration(businessRegistrationForm.fill(detailsTuple._2), service, displayDetails(service, true), redirectUrl, true))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][edit] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }

  }

  def update(service: String, redirectUrl : Option[String], isRegisterClient: Boolean) = AuthAction(service).async { implicit bcContext =>

    BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.update_business_registration(formWithErrors, service, displayDetails(service, isRegisterClient), redirectUrl, isRegisterClient)))
      },
      registerData => {
        businessRegistrationService.updateRegisterBusiness(registerData, isGroup = false, isNonUKClientRegisteredByAgent = true, service, isBusinessDetailsEditable = true).map { response =>
          redirectUrl match {
            case Some(serviceUrl) => Redirect(serviceUrl)
            case _ => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
          }
        }
      }
    )
  }

  private def displayDetails(service: String, isRegisterClient: Boolean)(implicit bcContext: BusinessCustomerContext) = {
    if (bcContext.user.isAgent){
      if (isRegisterClient) {
        BusinessRegistrationDisplayDetails("NUK",
          Messages("bc.non-uk-reg.header"),
          Messages("bc.non-uk-reg.sub-header"),
          Some(Messages("bc.non-uk-reg.lede.update-text")),
          BCUtils.getIsoCodeTupleList)
      } else {
          BusinessRegistrationDisplayDetails("NUK",
            Messages("bc.business-registration.agent.non-uk.header"),
            Messages("bc.business-registration.text.agent", service),
            None,
            BCUtils.getIsoCodeTupleList)
      }
    }
    else {
      BusinessRegistrationDisplayDetails("NUK",
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        Some(Messages("bc.business-registration.lede.update-text")),
        BCUtils.getIsoCodeTupleList)

    }
  }

}
