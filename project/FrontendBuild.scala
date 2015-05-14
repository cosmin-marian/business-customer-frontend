import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "business-customer-frontend"
  val appVersion = envOrElse("BUSINESS_CUSTOMER_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "0.7.0"    

  private val frontendBootstrapVersion = "0.5.1"
  private val govukTemplateVersion = "2.6.0"
  private val playUiVersion = "1.8.1"
  private val httpCachingClientVersion = "2.5.0"
  
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % "1.3.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "0.7.0",
    "uk.gov.hmrc" %% "url-builder" % "0.5.0",
    "uk.gov.hmrc" %% "play-config" % "1.0.0",
    "uk.gov.hmrc" %% "play-json-logger" % "1.0.0",
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "2.2.1" % scope,
        "org.scalatestplus" %% "play" % "1.2.0" % scope,
        "org.pegdown" % "pegdown" % "1.4.2",
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % "0.4.0"
      )
    }.test
  }

  def apply() = compile ++ Test()
}


