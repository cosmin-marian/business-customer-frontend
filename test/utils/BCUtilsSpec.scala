package utils

import org.scalatestplus.play.PlaySpec

class BCUtilsSpec extends PlaySpec {

  "BCUtils" must {
    "validateUTR" must {
      "given valid UTR return true" in {
        BCUtils.validateUTR(Some("1111111111")) must be(true)
        BCUtils.validateUTR(Some("1111111112")) must be(true)
        BCUtils.validateUTR(Some("8111111113")) must be(true)
        BCUtils.validateUTR(Some("6111111114")) must be(true)
        BCUtils.validateUTR(Some("4111111115")) must be(true)
        BCUtils.validateUTR(Some("2111111116")) must be(true)
        BCUtils.validateUTR(Some("2111111117")) must be(true)
        BCUtils.validateUTR(Some("9111111118")) must be(true)
        BCUtils.validateUTR(Some("7111111119")) must be(true)
        BCUtils.validateUTR(Some("5111111123")) must be(true)
        BCUtils.validateUTR(Some("3111111124")) must be(true)
      }
      "given invalid UTR return false" in {
        BCUtils.validateUTR(Some("2111111111")) must be(false)
        BCUtils.validateUTR(Some("211111111")) must be(false)
        BCUtils.validateUTR(Some("211111 111 ")) must be(false)
        BCUtils.validateUTR(Some("211111ab111 ")) must be(false)
      }
      "None as UTR return false" in {
        BCUtils.validateUTR(None) must be(false)
      }
    }
  }

}
