package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.{ExternalUrls, BusinessCustomerRegime}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth._

object AgentController extends AgentController {
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector

}

trait AgentController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val authConnector: AuthConnector

  def register(service: String) = AuthorisedForGG(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession map {
        reviewDetails =>
          reviewDetails.flatMap(_.agentReferenceNumber)  match {
          case Some(agentReferenceNumber) => Redirect(s"${ExternalUrls.agentConfirmationPath(service)}/${agentReferenceNumber}")
          case _ => throw new RuntimeException("AgentReferenceNumber not found")
        }
      }
  }
}
