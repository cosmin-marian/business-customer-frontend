package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ExternalUrlsSpec extends PlaySpec with OneServerPerSuite {

  "ExternalUrls" must {

    val service = "ATED"

    "have companyAuthHost " in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }

    "have loginCallback " in {
      ExternalUrls.loginCallback must be("http://localhost:9923/business-customer")
    }

    "have continueURL " in {
      ExternalUrls.continueURL(service) must be(s"http://localhost:9923/business-customer/$service")
    }

    "have loginPath " in {
      ExternalUrls.loginPath must be("sign-in")
    }

    "have loginURL " in {
      ExternalUrls.loginURL must be("http://localhost:9025/gg/sign-in")
    }

    "have signIn " in {
      ExternalUrls.signIn("ATED") must be(s"""http://localhost:9025/gg/sign-in?continue=http://localhost:9923/business-customer/ATED""")
    }

    "have signOut " in {
      ExternalUrls.signOut must be(s"""http://localhost:9025/gg/sign-out""")
    }

    "have serviceWelcomePath" in {
      ExternalUrls.serviceWelcomePath("ATED") must be("http://localhost:9916/ated/welcome")
      ExternalUrls.serviceWelcomePath("X") must be("#")
    }

    "have serviceAccountPath" in {
      ExternalUrls.serviceAccountPath("ATED") must be("#")

    }


  }
  
}
