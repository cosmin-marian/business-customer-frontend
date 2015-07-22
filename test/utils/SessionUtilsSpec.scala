package utils

import org.scalatestplus.play.PlaySpec

class SessionUtilsSpec extends PlaySpec {

  "SessionUtils" must {
    "getUniqueAckNo return 32 char long ack no" in {
      SessionUtils.getUniqueAckNo.length must be(32)
      SessionUtils.getUniqueAckNo.length must be(32)
    }
  }

}
