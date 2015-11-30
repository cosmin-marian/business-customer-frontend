package controllers

import config.FrontendAuthConnector
import connectors.DataCacheConnector
import controllers.auth.BusinessCustomerRegime
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
        reviewDetails => reviewDetails match {
          case Some(reviewData) => Ok(views.html.business_agent_confirmation(reviewData.agentReferenceNumber, service))
          case _ => throw new RuntimeException("AgentReferenceNumber not found")
        }
      }
  }
}
