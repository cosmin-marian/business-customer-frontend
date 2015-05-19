package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.{BusinessMatchDetails, ReviewDetails}
import play.api.libs.json.{JsString, JsObject, JsValue}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def matchBusiness(implicit user: AuthContext, hc: HeaderCarrier): Future[JsValue] = {

    getUserUtr.map{utr =>
      val details = BusinessMatchDetails(true, utr, None, None)
      val result = businessMatchingConnector.lookup(details)

      result flatMap {
        case reviewData =>
          dataCacheConnector.saveReviewDetails(reviewData.as[ReviewDetails]) map {
            data => reviewData
          }
      } recover {
        case _ => JsObject(Seq("error" -> JsString("Generic error")))
      }
    }.getOrElse(Future.successful(JsObject(Seq("error" -> JsString("Generic error")))))

  }

  def getUserUtr(implicit user: AuthContext) = {
    (user.principal.accounts.sa, user.principal.accounts.ct) match {
      case (Some(sa), x) => Some(sa.utr.utr.toString)
      case (None, Some(ct)) => Some(ct.utr.utr.toString)
      case _ => None
    }
  }
}
