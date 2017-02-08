package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
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
}

trait ReviewDetailsController extends BaseController with RunMode {

  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector

  def agentRegistrationService: AgentRegistrationService


  def businessDetails(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) =>
        if (bcContext.user.isAgent && businessDetails.isBusinessDetailsEditable) {
          Future.successful(Ok(views.html.review_details_non_uk_agent(serviceName, businessDetails)))
        } else {
          Future.successful(Ok(views.html.review_details(serviceName, bcContext.user.isAgent, businessDetails)))
        }
      case _ =>
        Logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
    }
  }

  def continue(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    if (bcContext.user.isAgent) {
      agentRegistrationService.enrolAgent(serviceName).map { response =>
        response.status match {
          case OK => Redirect(controllers.routes.AgentController.register(serviceName))
          case _ =>
            Logger.warn(s"[ReviewDetailsController][continue] - The service HMRC-AGENT-AGENT requires unique identifiers")
            throw new RuntimeException(Messages("bc.business-registration-error.duplicate.identifier"))
        }
      }
    } else {
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${serviceName.toLowerCase}.serviceRedirectUrl")
      serviceRedirectUrl match {
        case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
        case _ =>
          Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $serviceName")
          throw new RuntimeException(Messages("bc.business-review.error.no-service", serviceName, serviceName.toLowerCase))
      }
    }
  }

}
