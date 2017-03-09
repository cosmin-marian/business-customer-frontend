package controllers

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.ExternalUrls
import models.{EnrolErrorResponse, EnrolResponse}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.{Logger, Play}
import services.AgentRegistrationService
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.Future

object ExternalLinkController extends ExternalLinkController {

  val authConnector = FrontendAuthConnector
  override val controllerId: String = "ExternalLinkController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait ExternalLinkController extends BackLinkController with RunMode {

  def backLink(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    currentBackLink.map(backLink =>
      backLink match {
        case Some(x) => Redirect(x)
        case None => throw new RuntimeException(s"[ExternalLinkController][backLink] No Back Link found. Service: $serviceName")
      }
    )
  }
}
