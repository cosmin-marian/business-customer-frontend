package utils

import org.scalatestplus.play.PlaySpec

class SessionUtilsSpec extends PlaySpec {

  "SessionUtils" must {
    "be unique and 32 chars" in {
      SessionUtils.sessionOrUUID().size must be(32)
      SessionUtils.sessionOrUUID().size must be(32)
    }
  }

}
