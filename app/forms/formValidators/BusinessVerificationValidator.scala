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
      case "LTD" => !businessDetails.ltdCompany.ltdBusinessName.isEmpty
      case "UIB" =>  !businessDetails.uibCompany.uibBusinessName.isEmpty
      case "OBP" =>  !businessDetails.obpCompany.obpBusinessName.isEmpty
      case "LLP" =>  !businessDetails.llpCompany.llpBusinessName.isEmpty
      case _ => true
    }
  }

  def cotaxUTREmptyCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LTD" => !businessDetails.ltdCompany.ltdCotaxAUTR.isEmpty
      case "UIB" =>  !businessDetails.uibCompany.uibCotaxAUTR.isEmpty
      case _ => true
    }
  }

  def psaUTREmptyCheck(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "OBP" =>  !businessDetails.obpCompany.obpPSAUTR.isEmpty
      case "LLP" =>  !businessDetails.llpCompany.llpPSAUTR.isEmpty
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
        if (businessDetails.ltdCompany.ltdCotaxAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.ltdCompany.ltdCotaxAUTR.get.toString()))
      }
      case "UIB" =>  {
        if (businessDetails.uibCompany.uibCotaxAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.uibCompany.uibCotaxAUTR.get.toString()))
      }
      case _ => true
    }
  }

  def validatePSAUTR(businessDetails : BusinessDetails) = {
    businessDetails.businessType match {
      case "LLP" =>  {
        if (businessDetails.llpCompany.llpPSAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.llpCompany.llpPSAUTR.get.toString()))
      }
      case "OBP" =>  {
        if (businessDetails.obpCompany.obpPSAUTR.isEmpty) true
        else validateUTR(Some(businessDetails.obpCompany.obpPSAUTR.get.toString()))
      }
      case _ => true
    }
  }
}
