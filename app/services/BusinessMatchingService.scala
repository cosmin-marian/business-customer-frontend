package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.SessionUtils

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
        val searchData = MatchBusinessData(acknowledgmentReference = SessionUtils.getUniqueAckNo,
          utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = None)
        businessMatchingConnector.lookup(searchData, userType) flatMap {
          dataReturned =>
            validateAndCache(dataReturned = dataReturned)
        }
    }
  }

  def matchBusinessWithIndividualName(isAnAgent: Boolean, individual: Individual, saUTR: String)
                                     (implicit user: AuthContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgmentReference = SessionUtils.getUniqueAckNo,
      utr = saUTR, requiresNameMatch = true, isAnAgent = isAnAgent, individual = Some(individual), organisation = None)
    val userType = "sa"
    businessMatchingConnector.lookup(searchData, userType) flatMap {
      dataReturned =>
        validateAndCache(dataReturned = dataReturned)
    }
  }

  def matchBusinessWithOrganisationName(isAnAgent: Boolean, organisation: Organisation, utr: String)
                                       (implicit user: AuthContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgmentReference = SessionUtils.getUniqueAckNo,
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
    Logger.info(s"[BusinessMatchingService][validateAndCache]dataReturned = ${dataReturned}, isFailureResponse = ${isFailureResponse}")
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
    val addressReturned = getAddress(dataReturned)

    val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
      line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
      postcode = addressReturned.postalCode, country = addressReturned.countryCode)

    val reviewDetails = ReviewDetails(businessName = s"${individual.firstName} ${individual.lastName}",
      businessType = Some(businessType),
      businessAddress = address,
      sapNumber = getSapNumber(dataReturned),
      safeId = getSafeId(dataReturned),
      agentReferenceNumber = getAgentRefNum(dataReturned),
      firstName = Some(individual.firstName),
      lastName = Some(individual.lastName)
    )
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
      reviewDetailsReturned =>
        Future.successful(Json.toJson(reviewDetails))
    }
  }

  private def cacheOrg(dataReturned: JsValue)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val organisation = (dataReturned \ "organisation").as[OrganisationResponse]
    val businessType = organisation.organisationType
    val businessName = organisation.organisationName
    val addressReturned = getAddress(dataReturned)

    val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
      line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
      postcode = addressReturned.postalCode, country = addressReturned.countryCode)
    val reviewDetails = ReviewDetails(businessName = businessName,
      businessType = businessType,
      businessAddress = address,
      sapNumber = getSapNumber(dataReturned),
      safeId = getSafeId(dataReturned),
      agentReferenceNumber = getAgentRefNum(dataReturned))
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap {
      reviewDetailsReturned =>
        Future.successful(Json.toJson(reviewDetails))
    }
  }

  private def getAddress(dataReturned: JsValue): EtmpAddress = {
    val addressReturned = (dataReturned \ "address").as[Option[EtmpAddress]]
    addressReturned.getOrElse(throw new RuntimeException(s"[BusinessMatchingService][getAddress] - No Address returned from ETMP"))
  }

  private def getSafeId(dataReturned: JsValue): String = {
    val safeId = (dataReturned \ "safeId").as[Option[String]]
    safeId.getOrElse(throw new RuntimeException(s"[BusinessMatchingService][getSafeId] - No Safe Id returned from ETMP"))
  }

  private def getSapNumber(dataReturned: JsValue): String = {
    (dataReturned \ "sapNumber").as[Option[String]].getOrElse("")
  }

  private def getAgentRefNum(dataReturned: JsValue): Option[String] = {
    (dataReturned \ "agentReferenceNumber").as[Option[String]]
  }
}

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
