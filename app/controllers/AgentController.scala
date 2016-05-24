package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.ExternalUrls
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

object AgentController extends AgentController {
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector

}

trait AgentController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val authConnector: AuthConnector

  def register(service: String) = AuthAction(service).async {
    implicit bcContext =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession map {
        reviewDetails =>
          reviewDetails.flatMap(_.agentReferenceNumber) match {
            case Some(agentReferenceNumber) => Redirect(s"${ExternalUrls.agentConfirmationPath(service)}/${agentReferenceNumber}")
            case _ => throw new RuntimeException("AgentReferenceNumber not found")
          }
      }
  }
}
