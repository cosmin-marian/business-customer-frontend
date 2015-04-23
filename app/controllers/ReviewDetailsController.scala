package controllers

import java.util.UUID

import connectors.{AuthenticationConnectors, DataCacheConnector}
import play.api.mvc.{Request, Action}
import uk.gov.hmrc.play.frontend.controller.{ActionWithMdc, UnauthorisedAction, FrontendController}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  override val dataCacheConnector = DataCacheConnector
}

trait ReviewDetailsController extends FrontendController {

  def dataCacheConnector: DataCacheConnector

  def subscribe = Action {
    Ok(views.html.subscription())
  }

  def details = UnauthorisedAction.async { implicit request =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case businessDetails => Future.successful(Ok(views.html.review_details(businessDetails.get)))
    }
  }
}
