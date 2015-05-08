package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class BusinessCustomerGovernmentGatewaySpec extends PlaySpec with OneServerPerSuite {

  val serviceName: String = "ATED"
  
  "BusinessCustomerGovernmentGateway" must {
    
    "have login value overridden" in {
      BusinessCustomerGovernmentGateway(serviceName).login must be(ExternalUrls.signIn + s"/$serviceName")
    }
    
  }
  
}
