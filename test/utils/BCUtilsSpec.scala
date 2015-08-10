package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class BCUtilsSpec extends PlaySpec with OneServerPerSuite {

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

    "getSelectedCountry" must {
      "bring the correct country from the file" in {
        BCUtils.getSelectedCountry("GB") must be("United Kingdom")
        BCUtils.getSelectedCountry("US") must be("USA")
        BCUtils.getSelectedCountry("zz") must be(null)
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        BCUtils.getIsoCodeTupleList must contain(("US" , "USA"))
        BCUtils.getIsoCodeTupleList must contain(("GB" , "United Kingdom"))
      }
    }
    "getNavTitle" must {
      "for ated as service name, return ated" in {
        BCUtils.getNavTitle(Some("ated")) must be(Some("Annual Tax on Enveloped Dwellings (ATED)"))
      }
      "for awrs as service name, return awrs" in {
        BCUtils.getNavTitle(Some("awrs")) must be(Some("Alcohol Wholesaler Registration Scheme (AWRS)"))
      }
      "for other as service name, return None" in {
        BCUtils.getNavTitle(Some("abcd")) must be(None)
      }
      "for None as service name, return None" in {
        BCUtils.getNavTitle(None) must be(None)
      }
    }
  }

}
