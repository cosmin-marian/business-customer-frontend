package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{ReviewDetailsController, BackLinkController, BaseController}
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.config.RunMode
import utils.{OverseasCompanyUtils, BCUtils}

import scala.concurrent.Future

object OverseasCompanyRegController extends OverseasCompanyRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  override val businessRegistrationCache = BusinessRegCacheConnector
  override val controllerId: String = this.getClass.getName
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait OverseasCompanyRegController extends BackLinkController with RunMode {

  def businessRegistrationService: BusinessRegistrationService
  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service).async { implicit bcContext =>
    addBackLinkToPage(
      Ok(views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service,
        OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl))
    )
  }


  def register(service: String, addClient: Boolean, redirectUrl: Option[String] = None) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
      formWithErrors => {
        addBackLinkToPage(BadRequest(views.html.nonUkReg.overseas_company_registration(formWithErrors, service,
          OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl))
        )
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
          redirectPage <- redirectUrl match {
            case Some(x) => RedirectToExernal(x, controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient, redirectUrl))
            case None => RedirectWithBack(
              ReviewDetailsController.controllerId,
              controllers.routes.ReviewDetailsController.businessDetails(service),
              controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient, redirectUrl)
            )
          }
         } yield {
          redirectPage
        }
      }
    )
  }

}

