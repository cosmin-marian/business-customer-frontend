package controllers

import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
import play.api.Play
import uk.gov.hmrc.play.config.{RunMode, FrontendAuthConnector}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

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
        case businessDetails => Future.successful(Ok(views.html.review_details(serviceName, businessDetails.get)))
      }
  }

  def redirectToService(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
      serviceRedirectUrl match{
        case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
        case _ => throw new RuntimeException(s"Service does not exist for : $service. This should be in the conf file against 'govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl'")
      }
  }
}
