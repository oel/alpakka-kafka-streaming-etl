package alpakkafka.model

import akka.NotUsed
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContext, Future}

case class PropertyRating(
    propertyId: Int,
    affordability: Option[Int],
    comfort: Option[Int],
    neighborhood: Option[Int],
    schools: Option[Int]
  )

object PropertyRating {
  def rand = java.util.concurrent.ThreadLocalRandom.current

  def biasedRandNum(l: Int, u: Int, biasedNums: Set[Int], biasedFactor: Int = 1): Int = {
    Vector
      .iterate(rand.nextInt(l, u+1), biasedFactor)(_ => rand.nextInt(l, u+1))
      .dropWhile(!biasedNums.contains(_))
      .headOption match {
        case Some(n) => n
        case None => rand.nextInt(l, u+1)
      }
  }

  def fakeRating()(implicit ec: ExecutionContext): Future[Int] = Future{  // Fake rating computation
    Thread.sleep(biasedRandNum(1, 9, Set(3, 4, 5)))  // Sleep 1-9 secs
    biasedRandNum(1, 5, Set(2, 3, 4))  // Rating 1-5; mostly 2-4
  }

  def compute()(implicit ec: ExecutionContext): Flow[CommittableMessage[String, String], (PropertyRating, CommittableMessage[String, String]), NotUsed] =
    Flow[CommittableMessage[String, String]].mapAsync(1){ msg =>
      val propertyId = msg.record.key().toInt  // let it crash in case of bad PK data
      ( for {
            affordability <- PropertyRating.fakeRating()
            comfort <- PropertyRating.fakeRating()
            neighborhood <- PropertyRating.fakeRating()
            schools <- PropertyRating.fakeRating()
          }
          yield new PropertyRating(propertyId, Option(affordability), Option(comfort), Option(neighborhood), Option(schools))
        )
        .map(rating => (rating, msg)).recover{ case e => throw new Exception("ERROR in computeRatingFlow()!") }
    }
}
