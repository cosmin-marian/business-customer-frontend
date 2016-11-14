package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages

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
        BCUtils.getSelectedCountry("VG") must be("British Virgin Islands")
        BCUtils.getSelectedCountry("UG") must be("Uganda")
        BCUtils.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        BCUtils.getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        BCUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        BCUtils.getIsoCodeTupleList must contain(("UG", "Uganda"))
      }
    }


    "getNavTitle" must {
      "for ated as service name, return ated" in {
        BCUtils.getNavTitle(Some("ated")) must be(Some("Annual Tax on Enveloped Dwellings (ATED)"))
      }
      "for awrs as service name, return awrs" in {
        BCUtils.getNavTitle(Some("awrs")) must be(Some("Alcohol Wholesaler Registration Scheme (AWRS)"))
      }
      "for amls as service name, return amls" in {
        BCUtils.getNavTitle(Some("amls")) must be(Some("Anti Money Laundering Scheme (AMLS)"))
      }
      "for investment-tax-relief as service name, return investment-tax-relief" in {
        BCUtils.getNavTitle(Some("investment-tax-relief")) must be(Some("Apply for Enterprise Investment Scheme"))
      }
      "for other as service name, return None" in {
        BCUtils.getNavTitle(Some("abcd")) must be(None)
      }
      "for None as service name, return None" in {
        BCUtils.getNavTitle(None) must be(None)
      }
    }

    "businessTypeMap" must {

      "return the correct map for ated" in {
        val typeMap = BCUtils.businessTypeMap("ated", false)
        typeMap.size must be(5)
        typeMap.head._1 must be("LTD")
        typeMap(1)._1 must be("OBP")
      }

      "return the correct map for awrs" in {
        val typeMap = BCUtils.businessTypeMap("awrs", false)
        typeMap.size must be(7)
        typeMap.head._1 must be("GROUP")
        typeMap(1)._1 must be("SOP")
      }

      "return the correct map for amls" in {
        val typeMap = BCUtils.businessTypeMap("amls", false)
        typeMap.size must be(5)
        typeMap mustBe Seq(
          "LTD" -> Messages("bc.business-verification.LTD"),
          "SOP" -> Messages("bc.business-verification.amls.SOP"),
          "OBP" -> Messages("bc.business-verification.amls.PRT"),
          "LLP" -> Messages("bc.business-verification.amls.LP.LLP"),
          "UIB" -> Messages("bc.business-verification.amls.UIB")
        )
      }

      "return the correct map for investment-tax-relief" in {
        val typeMap = BCUtils.businessTypeMap("investment-tax-relief", false)
        typeMap.size must be(1)
        typeMap mustBe Seq(
          "LTD" -> Messages("bc.business-verification.LTD")
        )
      }

      "return default map when passed nothing" in {
        val typeMap = BCUtils.businessTypeMap("", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "SOP" -> Messages("bc.business-verification.SOP"),
          "LTD" -> Messages("bc.business-verification.LTD"),
          "OBP" -> Messages("bc.business-verification.PRT"),
          "LP" -> Messages("bc.business-verification.LP"),
          "LLP" -> Messages("bc.business-verification.LLP"),
          "UIB" -> Messages("bc.business-verification.UIB")
        )
      }
    }

  }

}
