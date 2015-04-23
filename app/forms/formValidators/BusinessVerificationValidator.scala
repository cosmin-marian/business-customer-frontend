package forms.formValidators

import forms.BusinessDetails
import utils.BCUtils._

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
  def saUTREmptyCheck(businessDetails : BusinessDetails) = {
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

  def cotaxUTREmptyCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LTD" => !businessDetails.limitedCompany.ltdCotaxUTR.isEmpty
      case "UIB" =>  !businessDetails.uib.uibCotaxUTR.isEmpty
      case _ => true
    }
  }

  def psaUTREmptyCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "OBP" =>  !businessDetails.obp.obpPSAUTR.isEmpty
      case "LLP" =>  !businessDetails.llp.llpPSAUTR.isEmpty
      case _ => true
    }
  }

  def validateSAUTR(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "SOP" =>  {
        if (businessDetails.soleTrader.sAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.soleTrader.sAUTR.get.toString()))
      }
      case _ => true
    }
  }

  def validateCOUTR(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LTD" =>  {
        if (businessDetails.limitedCompany.ltdCotaxUTR.isEmpty) true
        else validateUTR(Some(businessDetails.limitedCompany.ltdCotaxUTR.get.toString()))
      }
      case "UIB" =>  {
        if (businessDetails.uib.uibCotaxUTR.isEmpty) true
        else validateUTR(Some(businessDetails.uib.uibCotaxUTR.get.toString()))
      }
      case _ => true
    }
  }

  def validatePSAUTR(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LLP" =>  {
        if (businessDetails.llp.llpPSAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.llp.llpPSAUTR.get.toString()))
      }
      case "OBP" =>  {
        if (businessDetails.obp.obpPSAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.obp.obpPSAUTR.get.toString()))
      }
      case _ => true
    }
  }
}
