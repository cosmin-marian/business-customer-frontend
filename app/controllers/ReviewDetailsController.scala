package controllers

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.ExternalUrls
import models.{EnrolErrorResponse, EnrolResponse}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.{Logger, Play}
import services.AgentRegistrationService
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  val dataCacheConnector = DataCacheConnector
  val authConnector = FrontendAuthConnector
  val agentRegistrationService = AgentRegistrationService
  override val controllerId: String = "ReviewDetailsController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait ReviewDetailsController extends BackLinkController with RunMode {

  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector

  def agentRegistrationService: AgentRegistrationService


  def businessDetails(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) =>
        currentBackLink.map(backLink =>
          if (bcContext.user.isAgent && businessDetails.isBusinessDetailsEditable) {
            Ok(views.html.review_details_non_uk_agent(serviceName, businessDetails, backLink))
          } else {
            Ok(views.html.review_details(serviceName, bcContext.user.isAgent, businessDetails, backLink))
          }
        )
      case _ =>
        Logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
    }
  }

  def continue(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    if (bcContext.user.isAgent) {
      agentRegistrationService.enrolAgent(serviceName).flatMap { response =>
        response.status match {
          case OK => RedirectToExernal(ExternalUrls.agentConfirmationPath(serviceName), Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
          case BAD_GATEWAY =>
            Logger.warn(s"[ReviewDetailsController][continue] - The service HMRC-AGENT-AGENT requires unique identifiers")
            Future.successful(Ok(views.html.global_error(Messages("bc.business-registration-error.duplicate.identifier.header"),
              Messages("bc.business-registration-error.duplicate.identifier.title"),
              Messages("bc.business-registration-error.duplicate.identifier.message"), Some(serviceName))))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][continue] - Execption other tha status - OK and BAD_GATEWAY")
            throw new RuntimeException(Messages("bc.business-review.error.not-found"))

        }
      }
    } else {
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${serviceName.toLowerCase}.serviceRedirectUrl")
      serviceRedirectUrl match {
        case Some(serviceUrl) => RedirectToExernal(serviceUrl, Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
        case _ =>
          Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $serviceName")
          throw new RuntimeException(Messages("bc.business-review.error.no-service", serviceName, serviceName.toLowerCase))
      }
    }
  }

}
