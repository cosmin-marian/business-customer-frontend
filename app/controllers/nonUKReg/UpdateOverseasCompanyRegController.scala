package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BusinessRegCacheConnector
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
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils

import scala.concurrent.Future

object UpdateOverseasCompanyRegController extends UpdateOverseasCompanyRegController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  // $COVERAGE-ON$
}

trait UpdateOverseasCompanyRegController extends BaseController with RunMode {

  def businessRegistrationService: BusinessRegistrationService

  def viewForUpdate(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service).async { implicit bcContext =>
    Ok(views.html.nonUkReg.update_overseas_company_registration(overseasCompanyForm, service, bcContext.user.isAgent, addClient, BCUtils.getIsoCodeTupleList, redirectUrl))

    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.update_overseas_company_registration(overseasCompanyForm.fill(detailsTuple._3), service, bcContext.user.isAgent, addClient, BCUtils.getIsoCodeTupleList, redirectUrl))
          case _ =>
            Logger.warn(s"[UpdateOverseasCompanyRegController][viewForUpdate] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }
  }


  def update(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.update_overseas_company_registration(formWithErrors, service, bcContext.user.isAgent, addClient, BCUtils.getIsoCodeTupleList, redirectUrl)))
      },
      overseasCompany => {
        businessRegistrationService.getDetails.flatMap{
          businessDetails =>
            businessDetails match {
              case Some(detailsTuple) =>
                businessRegistrationService.updateRegisterBusiness(detailsTuple._2, overseasCompany, isGroup = false, isNonUKClientRegisteredByAgent = addClient, service, isBusinessDetailsEditable = true).map { response =>
                  redirectUrl match {
                    case Some(serviceUrl) => Redirect(serviceUrl)
                    case _ => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
                  }
                }
              case _ =>
                Logger.warn(s"[UpdateOverseasCompanyRegController][update] - No registration details found to edit")
                throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
            }
        }
      }
    )
  }
}

