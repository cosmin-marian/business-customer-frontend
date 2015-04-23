package controllers

import connectors.DataCacheConnector
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
}

trait ReviewDetailsController extends FrontendController {

  def dataCacheConnector: DataCacheConnector

  def businessDetails(serviceName: String) = UnauthorisedAction.async { implicit request =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case businessDetails => Future.successful(Ok(views.html.review_details(serviceName, businessDetails.get)))
    }
  }
}
