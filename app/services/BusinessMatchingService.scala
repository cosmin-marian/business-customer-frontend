package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
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
        businessMatchingConnector.lookup(searchData) flatMap {
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
        businessMatchingConnector.lookup(searchData) flatMap {
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
        businessMatchingConnector.lookup(searchData) flatMap {
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

  private def validateAndCache(dataReturned: JsValue)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val isFailureResponse = dataReturned.validate[MatchFailureResponse].isSuccess
    Logger.info(s" ###### dataReturned = ${dataReturned}       ~~~~~ isFailureResponse = ${isFailureResponse}")
    isFailureResponse match {
      case true => Future.successful(dataReturned)
      case false => {
        val isAnIndividual = (dataReturned \ "isAnIndividual").as[Boolean]
        isAnIndividual match {
          case true => {
            val businessType = "Sole Trader"
            val individual = (dataReturned \ "individual").as[Individual]
            val addressReturned = (dataReturned \ "address").as[EtmpAddress]
            val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
              line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
              postcode = addressReturned.postalCode, country = addressReturned.countryCode)
            val reviewDetails = ReviewDetails(businessName = s"${individual.firstName} ${individual.lastName}",
              businessType = businessType, businessAddress = address)
            dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
              reviewDetailsReturned =>
                Future.successful(Json.toJson(reviewDetails))
            }
          }
          case false => {
            val organisation = (dataReturned \ "organisation").as[Organisation]
            val businessType = organisation.organisationType
            val businessName = organisation.organisationName
            val addressReturned = (dataReturned \ "address").as[EtmpAddress]
            val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
              line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
              postcode = addressReturned.postalCode, country = addressReturned.countryCode)
            val reviewDetails = ReviewDetails(businessName = businessName, businessType = businessType, businessAddress = address)
            dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
              reviewDetailsReturned =>
                Future.successful(Json.toJson(reviewDetails))
            }
          }
        }
      }
    }
  }

}

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
