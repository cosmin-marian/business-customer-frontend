package forms.formValidators

import forms.BusinessDetails

object BusinessVerificationValidator {

  def saFirstNameCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "SOP" => !businessDetails.soleTrader.sAFirstName.isEmpty
      case _ => true
    }
  }
  def saSurnameCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "SOP" =>  !businessDetails.soleTrader.sASurname.isEmpty
      case _ => true
    }
  }
  def saUTRCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "SOP" => !businessDetails.soleTrader.sAUTR.isEmpty
      case _ => true
    }
  }

  def businessNameCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LTD" => !businessDetails.limitedCompany.ltdBusinessName.isEmpty
      case "UIB" =>  !businessDetails.uib.uibBusinessName.isEmpty
      case "OBP" =>  !businessDetails.obp.obpBusinessName.isEmpty
      case "LLP" =>  !businessDetails.llp.llpBusinessName.isEmpty
      case _ => true
    }
  }

  def cotaxUTRCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LTD" => !businessDetails.limitedCompany.ltdCotaxUTR.isEmpty
      case "UIB" =>  !businessDetails.uib.uibCotaxUTR.isEmpty
      case _ => true
    }
  }

  def psaUTRCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "OBP" =>  !businessDetails.obp.obpPSAUTR.isEmpty
      case "LLP" =>  !businessDetails.llp.llpPSAUTR.isEmpty
      case _ => true
    }
  }
}
