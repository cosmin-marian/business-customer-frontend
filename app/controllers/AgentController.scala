package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.ExternalUrls
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

object AgentController extends AgentController {
  val authConnector = FrontendAuthConnector
  val dataCacheConnector = DataCacheConnector

}

trait AgentController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def authConnector: AuthConnector

  def register(service: String) = AuthAction(service).async {
    implicit bcContext =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession map { reviewDetails =>
        reviewDetails.map(_.businessName) match {
          case Some(businessName) => Redirect(s"${ExternalUrls.agentConfirmationPath(service)}/$businessName")
          case _ => throw new RuntimeException("AgentReferenceNumber not found")
        }
      }
  }
}
