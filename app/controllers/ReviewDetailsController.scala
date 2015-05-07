package controllers

import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
}

trait ReviewDetailsController extends FrontendController with Actions {

  def dataCacheConnector: DataCacheConnector

  def businessDetails(serviceName: String) = AuthorisedFor(BusinessCustomerRegime(serviceName)) .async { implicit user => implicit request =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case businessDetails => Future.successful(Ok(views.html.review_details(serviceName, businessDetails.get)))
    }
  }
}
