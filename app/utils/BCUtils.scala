package utils

object BCUtils {

  def validateUTR(utr: Option[String]): Boolean = {
    utr match {
      case Some(utr) => {
        utr.trim.length == 10 && utr.trim.forall(_.isDigit) && {
          val actualUtr = utr.trim.toList
          val checkDigit = actualUtr.head.asDigit
          val restOfUtr = actualUtr.tail
          val weights = List(6, 7, 8, 9, 10, 5, 4, 3, 2)
          val weightedUtr = for((w1,u1) <- weights zip restOfUtr) yield {
            w1*(u1.asDigit)
          }
          val total = weightedUtr.sum
          println(total)
          val remainder = total % 11
          println(remainder + " " + checkDigit)
          isValidUtr(remainder, checkDigit)
        }
      }
      case None => false
    }
  }
  private def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
    remainder match {
      case 0 => checkDigit == 2
      case 1 => checkDigit == 1
      case 2 => checkDigit == 9
      case 3 => checkDigit == 8
      case 4 => checkDigit == 7
      case 5 => checkDigit == 6
      case 6 => checkDigit == 5
      case 7 => checkDigit == 4
      case 8 => checkDigit == 3
      case 9 => checkDigit == 2
      case 10 => checkDigit == 1
    }
  }

}
