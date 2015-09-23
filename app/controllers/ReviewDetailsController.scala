package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
import play.api.i18n.Messages
import play.api.{Logger, Play}
import services.AgentRegistrationService
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.AuthUtils

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
  override val agentRegistrationService = AgentRegistrationService

}

trait ReviewDetailsController extends FrontendController with Actions with RunMode {

  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector

  def agentRegistrationService: AgentRegistrationService


  def businessDetails(serviceName: String) = AuthorisedFor(BusinessCustomerRegime(serviceName)).async {
    implicit user => implicit request =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
        case Some(businessDetails) => Future.successful(Ok(views.html.review_details(serviceName, AuthUtils.isAgent, businessDetails)))
        case _ => {
          Logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for")
          throw new RuntimeException(Messages("bc.business-review.error.not-found"))
        }
      }
  }

  def continue(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      AuthUtils.isAgent match {
        case false => {
          val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
          serviceRedirectUrl match {
            case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
            case _ => {
              Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = ${service}")
              throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
            }
          }
        }
        case true => {
          agentRegistrationService.enrolAgent(service).map { response =>
            Redirect(controllers.routes.AgentController.register(service))
          }
        }
      }
  }


}
