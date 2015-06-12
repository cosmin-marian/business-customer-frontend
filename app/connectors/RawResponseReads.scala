package connectors

import uk.gov.hmrc.play.http.{HttpResponse, HttpReads}

trait RawResponseReads {
  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }
}
