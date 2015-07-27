package services

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.logging.SessionId

import scala.concurrent.Future

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val testAddress = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire"), Some("NE98 1ZZ"), "UK")
  val testReviewDetails = ReviewDetails("ACME", Some("Limited"), testAddress, "1234567890", "EX0012345678909", "01234567890")
  val matchFailureResponse = MatchFailureResponse(Reason = "Sorry. Business details not found. Try with correct UTR and/or name.")
  val matchFailureResponseJson = Json.toJson(matchFailureResponse)
  val successOrgJson = Json.parse( """{"sapNumber":"1234567890","safeId":"EX0012345678909","agentReferenceNumber":"01234567890", "isEditable":true,"isAnAgent":false,"isAnIndividual":false, "organisation":{"organisationName":"Real Business Inc","isAGroup":true,"organisationType":"unincorporated body"},
"address":{"addressLine1":"23 High Street","addressLine2":"Park View", "addressLine3":"Gloucester","addressLine4":"Gloucestershire","postalCode":"NE98 1ZZ","countryCode":"UK"}, "contactDetails":{"phoneNumber":"1234567890"}}""")
  val successOrgReviewDetails = ReviewDetails("Real Business Inc", Some("unincorporated body"), testAddress, "1234567890", "EX0012345678909", "01234567890")
  val successOrgReviewDetailsJson = Json.toJson(successOrgReviewDetails)
  val successIndividualJson = Json.parse( """{"sapNumber":"1234567890", "safeId":"EX0012345678909", "agentReferenceNumber":"01234567890", "isEditable":true, "isAnAgent":false, "isAnIndividual":true, "individual":{"firstName":"first name", "lastName":"last name"}, "address":{"addressLine1":"23 High Street","addressLine2":"Park View", "addressLine3":"Gloucester","addressLine4":"Gloucestershire","postalCode":"NE98 1ZZ","countryCode":"UK"}, "contactDetails":{"phoneNumber":"1234567890"}}""")
  val successIndReviewDetails = ReviewDetails("first name last name", Some("Sole Trader"), testAddress, "1234567890", "EX0012345678909", "01234567890", Some("first name"), Some("last name"))
  val successIndReviewDetailsJson = Json.toJson(successIndReviewDetails)
  val errorJson = Json.parse( """{"error" : "Some Error"}""")
  val utr = "1234567890"
  val noMatchUtr = "9999999999"
  val testIndividual = Individual("firstName", "lastName", None)
  val testOrganisation = Organisation("org name", "org type")
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))


  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestBusinessMatchingService extends BusinessMatchingService {
    val businessMatchingConnector: BusinessMatchingConnector = mockBusinessMatchingConnector
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockBusinessMatchingConnector)
    reset(mockDataCacheConnector)
  }

  "BusinessMatchingService" must {
    "matchBusinessWithUTR" must {
      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        await(result.get) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match found with CT user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(ct = Some(CtAccount(s"/ct/organisation/$utr", CtUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(successOrgJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        await(result.get) must be(successOrgReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with CT user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(ct = Some(CtAccount(s"/ct/organisation/$utr", CtUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for ORG user, return None as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(org = Some(OrgAccount("", Org("1234")))), None, None))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for user with Both SA & CT, return None as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount("sa/1234", SaUtr("1111111111"))), ct = Some(CtAccount("ct/1234", CtUtr("1111111111")))), None, None))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }
    }

    "matchBusinessWithIndividualName" must {
      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }
    }

    "matchBusinessWithOrganisationName" must {
      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }
    }
  }

}
