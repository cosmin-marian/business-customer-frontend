package controllers

import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
import play.api.Play
import play.api.i18n.Messages
import services.SubscriptionDetailsService
import uk.gov.hmrc.play.config.RunMode
import config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.InternalServerException

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
  override val subscriptionDetailsService = SubscriptionDetailsService
}

trait ReviewDetailsController extends FrontendController with Actions with RunMode {
  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector
  val subscriptionDetailsService: SubscriptionDetailsService

  def businessDetails(serviceName: String) = AuthorisedFor(BusinessCustomerRegime(serviceName)).async {
    implicit user => implicit request =>
      subscriptionDetailsService.fetchSubscriptionDetails.flatMap{subscriptionDetails =>
        dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
          case Some(businessDetails) => Future.successful(Ok(views.html.review_details(subscriptionDetails, businessDetails)))
          case _ => throw new InternalServerException(Messages("bc.business-review.error.not-found"))
        }
      }

  }

  def redirectToService(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      subscriptionDetailsService.fetchSubscriptionDetails.flatMap { subscriptionDetails =>
        val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${subscriptionDetails.service.toLowerCase}.serviceRedirectUrl")
        serviceRedirectUrl match {
          case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
          case _ => throw new RuntimeException(s"Service does not exist for :" +
            s" $service. This should be in the conf file against 'govuk-tax.$env.services.${subscriptionDetails.service.toLowerCase}.serviceRedirectUrl'")
        }
      }
  }
}
