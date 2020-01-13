package nl.knaw.dans.easy.properties.app.model

import scala.Ordering.Implicits._

abstract sealed class TimeFilter
case class EarlierThan(timestamp: Timestamp) extends TimeFilter
case class LaterThan(timestamp: Timestamp) extends TimeFilter
case class AtTime(timestamp: Timestamp) extends TimeFilter
case class Between(earlier: Timestamp, later: Timestamp) extends TimeFilter
case class NotBetween(earlier: Timestamp, later: Timestamp) extends TimeFilter

object TimeFilter {
  def apply(earlierThan: Option[Timestamp],
            laterThan: Option[Timestamp],
            atTimestamp: Option[Timestamp],
           ): Option[TimeFilter] = {
    (earlierThan, laterThan, atTimestamp) match {
      case (None, None, None) => Option.empty
      case (Some(earlierThan), None, None) => Option(EarlierThan(earlierThan))
      case (None, Some(laterThan), None) => Option(LaterThan(laterThan))
      case (Some(earlierThan), Some(laterThan), None) => Option(between(earlierThan, laterThan))
      case (None, None, Some(atTimestamp)) => Option(AtTime(atTimestamp))
      case (None, Some(_), Some(_)) |
           (Some(_), None, Some(_)) |
           (Some(_), Some(_), Some(_)) =>
        throw new IllegalArgumentException("argument 'atTimestamp' cannot be used in conjunction with arguments 'earlierThan' or 'laterThan'")
    }
  }

  private def between(earlierThan: Timestamp, laterThan: Timestamp): TimeFilter = {
    if (earlierThan equiv laterThan)
      throw new IllegalArgumentException("arguments 'earlierThan' and 'laterThan' cannot have the same value; use 'atTimestamp' instead")
    else if (earlierThan > laterThan) {
      Between(earlierThan, laterThan)
    }
    else
      NotBetween(earlierThan, laterThan)
  }
}
