package controllers.nonUKReg

import config.FrontendAuthConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistrationDisplayDetails}
import play.api.Logger
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

  def edit(service: String, redirectUrl : Option[String]) = AuthAction(service).async { implicit bcContext =>
    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.update_business_registration(businessRegistrationForm.fill(detailsTuple._2), service, displayDetails(service), redirectUrl))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][edit] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }

  }

  def update(service: String, redirectUrl : Option[String]) = AuthAction(service).async { implicit bcContext =>

    BusinessRegistrationForms.validateNonUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.update_business_registration(formWithErrors, service, displayDetails(service), redirectUrl)))
      },
      registerData => {
        businessRegistrationService.updateRegisterBusiness(registerData, isGroup = false, isNonUKClientRegisteredByAgent = true, service).map { response =>
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

  private def displayDetails(service: String)(implicit bcContext: BusinessCustomerContext) = {
    if (bcContext.user.isAgent){
      BusinessRegistrationDisplayDetails("NUK",
        Messages("bc.non-uk-reg.header"),
        Messages("bc.non-uk-reg.sub-header"),
        Messages("bc.non-uk-reg.lede.update-text"),
        BCUtils.getIsoCodeTupleList)
    }
    else {
      BusinessRegistrationDisplayDetails("NUK",
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        Messages("bc.business-registration.lede.update-text"),
        BCUtils.getIsoCodeTupleList)

    }
  }

}
