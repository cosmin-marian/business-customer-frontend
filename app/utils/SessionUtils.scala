package utils

import java.util.UUID
import uk.gov.hmrc.play.audit.http.HeaderCarrier

object SessionUtils {

  def sessionOrUUID(implicit hc: HeaderCarrier): String = {
    hc.sessionId match {
      case Some(sessionId) => sessionId.value
      case None => UUID.randomUUID().toString.replace("-", "")
    }
  }
}
