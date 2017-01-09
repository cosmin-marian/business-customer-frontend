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
import utils.BCUtils

import scala.concurrent.Future

object OverseasCompanyRegController extends OverseasCompanyRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  override val businessRegistrationCache = BusinessRegCacheConnector
}

trait OverseasCompanyRegController extends BaseController with RunMode {

  def businessRegistrationService: BusinessRegistrationService
  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service) { implicit bcContext =>
    Ok(views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, bcContext.user.isAgent, addClient, BCUtils.getIsoCodeTupleList, redirectUrl))
  }


  def send(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.nonUkReg.overseas_company_registration(formWithErrors, service, bcContext.user.isAgent, addClient, BCUtils.getIsoCodeTupleList, redirectUrl)))
      },
      overseasCompany => {
        for {
          cachedBusinessReg <- businessRegistrationCache.fetchAndGetBusinessRegForSession
          reviewDetails <-
            cachedBusinessReg match {
              case Some(businessReg) =>
                businessRegistrationService.registerBusiness(businessReg, overseasCompany, isGroup = false, isNonUKClientRegisteredByAgent = addClient, service, isBusinessDetailsEditable = true)
              case None =>
                throw new RuntimeException(s"[OverseasCompanyRegController][send] - service :$service. Error : No Cached BusinessRegistration")
            }
         } yield {
          redirectUrl match {
            case Some(x) => Redirect(x)
            case None => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
          }

        }
      }
    )
  }
}

