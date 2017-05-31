package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import play.api.Logger
import play.api.libs.json.Json


object BusinessCustomerController extends BusinessCustomerController {
  // $COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector
  // $COVERAGE-ON$
}

trait BusinessCustomerController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def clearCache(service: String) = AuthAction(service).async { implicit bcContext =>
    dataCacheConnector.clearCache.map { x =>
      x.status match {
        case OK | NO_CONTENT =>
          Ok
        case errorStatus => {
          Logger.error(s"session has not been cleared for $service. Status: $errorStatus, Error: ${x.body}")
          InternalServerError
        }
      }
    }
  }

  def getReviewDetails(service: String) = AuthAction(service).async { implicit bcContext =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession.map {
      case Some(businessDetails) =>
        Ok(Json.toJson(businessDetails))
      case _ =>
        Logger.warn(s"could not retrieve business details for $service")
        NotFound
    }
  }


}
