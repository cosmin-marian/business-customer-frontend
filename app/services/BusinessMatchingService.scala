package services

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.SessionUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessMatchingService {

  def businessMatchingConnector: BusinessMatchingConnector

  def dataCacheConnector: DataCacheConnector

  def matchBusinessWithUTR(isAnAgent: Boolean, service: String)
                          (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Option[Future[JsValue]] = {
    getUserUtrAndType map { userUtrAndType =>
      val (userUTR, userType) = userUtrAndType
      val searchData = MatchBusinessData(acknowledgementReference = SessionUtils.getUniqueAckNo,
        utr = userUTR, requiresNameMatch = false, isAnAgent = isAnAgent, individual = None, organisation = None)
      businessMatchingConnector.lookup(searchData, userType, service) flatMap { dataReturned =>
        validateAndCache(dataReturned = dataReturned, directMatch = true, utr = Some(userUTR))
      }
    }
  }

  def matchBusinessWithIndividualName(isAnAgent: Boolean, individual: Individual, saUTR: String, service: String)
                                     (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionUtils.getUniqueAckNo,
      utr = saUTR, requiresNameMatch = true, isAnAgent = isAnAgent, individual = Some(individual), organisation = None)
    val userType = "sa"
    businessMatchingConnector.lookup(searchData, userType, service) flatMap { dataReturned =>
      validateAndCache(dataReturned = dataReturned, directMatch = false, utr = Some(saUTR))
    }
  }

  def matchBusinessWithOrganisationName(isAnAgent: Boolean, organisation: Organisation, utr: String, service: String)
                                       (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[JsValue] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionUtils.getUniqueAckNo,
      utr = utr, requiresNameMatch = true, isAnAgent = isAnAgent, individual = None, organisation = Some(organisation))
    val userType = "org"
    businessMatchingConnector.lookup(searchData, userType, service) flatMap { dataReturned =>
      validateAndCache(dataReturned = dataReturned, directMatch = false, Some(utr))
    }
  }

  private def getUserUtrAndType(implicit bcContext: BusinessCustomerContext): Option[(String, String)] = {
    (bcContext.user.authContext.principal.accounts.sa, bcContext.user.authContext.principal.accounts.ct) match {
      case (Some(sa), None) => Some((sa.utr.utr.toString, "sa"))
      case (None, Some(ct)) => Some((ct.utr.utr.toString, "org"))
      case _ => None
    }
  }

  private def validateAndCache(dataReturned: JsValue, directMatch: Boolean, utr: Option[String])(implicit hc: HeaderCarrier): Future[JsValue] = {
    val isFailureResponse = dataReturned.validate[MatchFailureResponse].isSuccess
    if (isFailureResponse) Future.successful(dataReturned)
    else {
      val isAnIndividual = (dataReturned \ "isAnIndividual").as[Boolean]
      if (isAnIndividual) cacheIndividual(dataReturned, directMatch, utr)
      else cacheOrg(dataReturned, directMatch, utr)
    }
  }


  private def cacheIndividual(dataReturned: JsValue, directMatch: Boolean, utr: Option[String])(implicit hc: HeaderCarrier): Future[JsValue] = {
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
      //default value from model due to AWRS
      //      isAGroup = false,
      directMatch = directMatch,
      firstName = Some(individual.firstName),
      lastName = Some(individual.lastName),
      utr = utr
    )
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap { reviewDetailsReturned =>
      Future.successful(Json.toJson(reviewDetails))
    }
  }

  private def cacheOrg(dataReturned: JsValue, directMatch: Boolean, utr: Option[String])(implicit hc: HeaderCarrier): Future[JsValue] = {
    val organisation = (dataReturned \ "organisation").as[OrganisationResponse]
    val businessType = organisation.organisationType
    val businessName = organisation.organisationName
    val isAGroup = organisation.isAGroup
    val addressReturned = getAddress(dataReturned)
    val address = Address(line_1 = addressReturned.addressLine1, line_2 = addressReturned.addressLine2,
      line_3 = addressReturned.addressLine3, line_4 = addressReturned.addressLine4,
      postcode = addressReturned.postalCode, country = addressReturned.countryCode)
    val reviewDetails = ReviewDetails(businessName = businessName,
      businessType = businessType,
      isAGroup = isAGroup.getOrElse(false),
      directMatch = directMatch,
      businessAddress = address,
      sapNumber = getSapNumber(dataReturned),
      safeId = getSafeId(dataReturned),
      agentReferenceNumber = getAgentRefNum(dataReturned),
      utr = utr)
    dataCacheConnector.saveReviewDetails(reviewDetails) flatMap { reviewDetailsReturned =>
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
