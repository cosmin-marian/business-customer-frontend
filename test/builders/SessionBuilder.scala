package builders

import java.util.UUID

import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.SessionKeys

object SessionBuilder {

  val TOKEN = "token" // this is because SessionKeys.token gives warning

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsJson], userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  def buildRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  def buildRequestWithSessionNoUser() = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId)
  }

}
