package config

import java.io.File

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Play._
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport

import scala.collection.JavaConversions._

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = BusinessCustomerFrontendAuditConnector
  override val loggingFilter = BusinessCustomerFrontendLoggingFilter
  override val frontendAuditFilter = BusinessCustomerFrontendAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    val url = request.path
    val serviceList =  configuration.getStringList(s"govuk-tax.$env.services.names").getOrElse(throw new Exception("No services available in application configuration"))
    val serviceName = serviceList.filter(url.toLowerCase.contains(_)).headOption
    views.html.global_error(pageTitle, heading, message, service = serviceName)(request, applicationMessages)
  }
  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"govuk-tax.$env.metrics")

}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object BusinessCustomerFrontendLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport  {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object BusinessCustomerFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport  {

  override lazy val maskedFormFields = Seq.empty

  override lazy val applicationPort = None

  override lazy val auditConnector = BusinessCustomerFrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

