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
    getUserUtrAndType map {
      userUtrAndType =>
        val (userUTR, userType) = userUtrAndType
        val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = None)
        businessMatchingConnector.lookup(searchData, userType) flatMap {
          dataReturned =>
            validateAndCache(dataReturned = dataReturned)
        }
    }
  }

  def matchBusinessWithIndividualName(isAnAgent: Boolean, individual: Individual, saUTR: String)
                                     (implicit user: AuthContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
      utr = saUTR, requiresNameMatch = true, isAnAgent = isAnAgent, individual = Some(individual), organisation = None)
    val userType = "sa"
    businessMatchingConnector.lookup(searchData, userType) flatMap {
      dataReturned =>
        validateAndCache(dataReturned = dataReturned)
    }
  }

  def matchBusinessWithOrganisationName(isAnAgent: Boolean, organisation: Organisation, utr: String)
                                       (implicit user: AuthContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionKeys.sessionId,
      utr = utr, requiresNameMatch = true, isAnAgent = isAnAgent, individual = None, organisation = Some(organisation))
    val userType = "org"
    businessMatchingConnector.lookup(searchData, userType) flatMap {
      dataReturned =>
        validateAndCache(dataReturned = dataReturned)
    }
  }

  private def getUserUtrAndType(implicit user: AuthContext): Option[(String, String)] = {
    (user.principal.accounts.sa, user.principal.accounts.ct) match {
      case (Some(sa), None) => Some((sa.utr.utr.toString, "sa"))
      case (None, Some(ct)) => Some((ct.utr.utr.toString, "org"))
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
            cacheIndividual(dataReturned)
          }
          case false => {
            cacheOrg(dataReturned)
          }
        }
      }
    }
  }

  private def cacheIndividual(dataReturned: JsValue)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val businessType = "Sole Trader"
    val individual = (dataReturned \ "individual").as[Individual]
    val addressReturned = (dataReturned \ "address").as[EtmpAddress]
    val sapNumber = (dataReturned \ "sapNumber").as[String]
    val safeId = (dataReturned \ "safeId").as[String]
    val agentReferenceNumber = (dataReturned \ "agentReferenceNumber").as[String]

    val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
      line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
      postcode = addressReturned.postalCode, country = addressReturned.countryCode)
    val reviewDetails = ReviewDetails(businessName = s"${individual.firstName} ${individual.lastName}",
      businessType = businessType, businessAddress = address,
      sapNumber = sapNumber,
      safeId = safeId,
      agentReferenceNumber = agentReferenceNumber,
      firstName = Some(individual.firstName),
      lastName = Some(individual.lastName)
    )
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
      reviewDetailsReturned =>
        Future.successful(Json.toJson(reviewDetails))
    }
  }

  private def cacheOrg(dataReturned: JsValue)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val organisation = (dataReturned \ "organisation").as[Organisation]
    val businessType = organisation.organisationType
    val businessName = organisation.organisationName
    val addressReturned = (dataReturned \ "address").as[EtmpAddress]
    val sapNumber = (dataReturned \ "sapNumber").as[String]
    val safeId = (dataReturned \ "safeId").as[String]
    val agentReferenceNumber = (dataReturned \ "agentReferenceNumber").as[String]

    val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
      line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
      postcode = addressReturned.postalCode, country = addressReturned.countryCode)
    val reviewDetails = ReviewDetails(businessName = businessName,
      businessType = businessType,
      businessAddress = address,
      sapNumber = sapNumber,
      safeId = safeId,
      agentReferenceNumber = agentReferenceNumber)
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
      reviewDetailsReturned =>
        Future.successful(Json.toJson(reviewDetails))
    }
  }

}

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
