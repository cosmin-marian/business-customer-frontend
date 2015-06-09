package services

import models.{Address, ReviewDetails}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.HeaderCarrier

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"),Some("NE98 1ZZ"), "U.K.")
  val reviewDetails = ReviewDetails("ACME", "UIB", address)
  val reviewDetailsJson = Json.toJson(reviewDetails)
  val utr = "1234567890"
  val noMatchUtr = "9999999999"
  implicit val hc = HeaderCarrier()

  "BusinessMatchingService" must {

  }

}
