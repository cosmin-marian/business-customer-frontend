package controllers.nonUKReg

import config.FrontendAuthConnector
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{Address, BusinessRegistration}
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.config.RunMode
import utils.BCUtils

import scala.concurrent.Future

object OverseasCompanyRegController extends OverseasCompanyRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
}

trait OverseasCompanyRegController extends BaseController with RunMode {

  def businessRegistrationService: BusinessRegistrationService

  def view(service: String) = AuthAction(service) { implicit bcContext =>
    Ok(views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, BCUtils.getIsoCodeTupleList))
  }


  def send(service: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.overseas_company_registration(formWithErrors, service, BCUtils.getIsoCodeTupleList)))
      },
      overseasCompany => {
        businessRegistrationService.registerBusiness(BusinessRegistration("", Address("","", None, None, None, "")), overseasCompany, isGroup = false, isNonUKClientRegisteredByAgent = false, service, isBusinessDetailsEditable = true).map {
          registrationSuccessResponse =>
            Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
        }
      }
    )
  }
}

