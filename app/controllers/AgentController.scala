package controllers

import config.FrontendAuthConnector
import controllers.auth.BusinessCustomerRegime
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
import connectors.DataCacheConnector


object AgentController extends AgentController {
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector

}

trait AgentController extends FrontendController with Actions {


  val dataCacheConnector: DataCacheConnector
  val authConnector: AuthConnector

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession map {
        reviewDetails => reviewDetails match {
          case Some(reviewData) => Ok(views.html.business_agent_confirmation(reviewData.agentReferenceNumber))
          case _ => throw new RuntimeException("AgentReferenceNumber not found")
        }
      }
  }
}
