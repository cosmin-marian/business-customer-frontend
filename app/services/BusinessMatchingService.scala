package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def matchBusinessWithUTR(isAnAgent: Boolean)
                          (implicit user: AuthContext, hc: HeaderCarrier): Option[Future[JsValue]] = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = None)
        businessMatchingConnector.lookup(searchData) map {
          dataReturned =>
            validateAndCache(dataReturned = dataReturned)
        }
    }
  }

  def matchBusinessWithIndividualName(isAnAgent: Boolean, individual: Individual)
                                     (implicit user: AuthContext, hc: HeaderCarrier): Option[Future[JsValue]] = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = Some(individual), organisation = None)
        businessMatchingConnector.lookup(searchData) map {
          dataReturned =>
            validateAndCache(dataReturned = dataReturned)
        }
    }
  }

  def matchBusinessWithOrganisationName(isAnAgent: Boolean, organisation: Organisation)
                                       (implicit user: AuthContext, hc: HeaderCarrier): Option[Future[JsValue]] = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = Some(organisation))
        businessMatchingConnector.lookup(searchData) map {
          dataReturned =>
            validateAndCache(dataReturned = dataReturned)
        }
    }
  }

  private def getUserUtr(implicit user: AuthContext): Option[String] = {
    (user.principal.accounts.sa, user.principal.accounts.ct) match {
      case (Some(sa), x) => Some(sa.utr.utr.toString)
      case (None, Some(ct)) => Some(ct.utr.utr.toString)
      case _ => None
    }
  }

  private def validateAndCache(dataReturned: JsValue)(implicit hc: HeaderCarrier): JsValue = {
    val isFailureResponse = dataReturned.validate[MatchFailureResponse].isSuccess
    isFailureResponse match {
      case true => dataReturned
      case false => {
        val isAnIndividual = (dataReturned \ "isAnIndividual").as[Boolean]
        isAnIndividual match {
          case true => {
            val businessType = "Sole Trader"
            val individual = (dataReturned \ "individual").as[Individual]
          }
          case false => {

          }
        }
      }
    }
    dataReturned.validate[ReviewDetails] match {
      case success: JsSuccess[ReviewDetails] => {
        success map {
          reviewDetailsReturned =>
            dataCacheConnector.saveReviewDetails(reviewDetailsReturned)
        }
        dataReturned
      }
      case failure: JsError => dataReturned
    }
  }

  private def convertToReviewDetails(dataReturned: JsValue): JsValue = {
    dataReturned
  }

}

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
