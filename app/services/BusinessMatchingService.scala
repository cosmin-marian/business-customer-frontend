package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models.{ReviewDetails, Individual, MatchBusinessData, Organisation}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.SessionKeys

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessMatchingService {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def matchBusinessWithUTR(isAnAgent: Boolean)(implicit user: AuthContext, hc: HeaderCarrier) = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = None)
        businessMatchingConnector.lookup(searchData) map {
          dataReturned =>
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
    }
  }

  def matchBusinessWithIndividualName(isAnAgent: Boolean, individual: Individual)(implicit user: AuthContext, hc: HeaderCarrier) = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = Some(individual), organisation = None)
        businessMatchingConnector.lookup(searchData) map {
          response =>
            response
        }
    }
  }

  def matchBusinessWithOrganisationName(isAnAgent: Boolean, organisation: Organisation)(implicit user: AuthContext, hc: HeaderCarrier) = {
    getUserUtr map {
      userUTR =>
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = Some(organisation))
        businessMatchingConnector.lookup(searchData) map {
          response =>
            response
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

}
