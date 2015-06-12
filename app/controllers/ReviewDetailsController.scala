package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
import play.api.Play
import play.api.i18n.Messages
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.AuthUtils

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
}

trait ReviewDetailsController extends FrontendController with Actions with RunMode {

  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector

  def businessDetails(serviceName: String) = AuthorisedFor(BusinessCustomerRegime(serviceName)).async {
    implicit user => implicit request =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
        case Some(businessDetails) => Future.successful(Ok(views.html.review_details(serviceName, AuthUtils.isAgent, businessDetails)))
        case _ => throw new RuntimeException(Messages("bc.business-review.error.not-found"))
      }
  }

  def continue(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      AuthUtils.isAgent match {
        case false => {
          val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
          serviceRedirectUrl match {
            case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
            case _ => throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
          }
        }
        case true => Future.successful(Redirect(controllers.routes.AgentController.register(service)))
      }
  }
}
