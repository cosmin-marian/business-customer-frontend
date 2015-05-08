package connectors


import config.WSHttp
import models.{BusinessMatchDetails, ReviewDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}

import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("business-matching")
  val baseURI = "business-matching"
  val lookupURI = "business-lookup"

  val http: HttpGet with HttpPost = WSHttp

  def lookup(lookupData: BusinessMatchDetails)(implicit headerCarrier: HeaderCarrier): Future[ReviewDetails] = {
    http.POST[BusinessMatchDetails, ReviewDetails]( s"""$serviceURL/$baseURI/$lookupURI""", lookupData)
  }
}

object BusinessMatchingConnector extends BusinessMatchingConnector