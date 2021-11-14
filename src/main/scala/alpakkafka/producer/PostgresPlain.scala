package alpakkafka.producer

import alpakkafka.model.PropertyListing

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object PostgresPlain {

  def runPropertyListing(topic: String,
                         offset: Int = 0,
                         limit: Int = Int.MaxValue)(implicit
                                         slickSession: SlickSession,
                                         jsonFormat: JsonFormat[PropertyListing],
                                         system: ActorSystem,
                                         ec: ExecutionContext): Future[Done] = {

    import slickSession.profile.api._

    class PropertyListings(tag: Tag) extends Table[PropertyListing](tag, "property_listing") {
      def propertyId = column[Int]("property_id", O.PrimaryKey)
      def dataSource = column[Option[String]]("data_source")
      def bathrooms = column[Option[Double]]("bathrooms")
      def bedrooms = column[Option[Int]]("bedrooms")
      def listPrice = column[Option[Double]]("list_price")
      def livingArea = column[Option[Int]]("living_area")
      def propertyType = column[Option[String]]("property_type")
      def yearBuilt = column[Option[String]]("year_built")
      def lastUpdated = column[Option[String]]("last_updated")
      def streetAddress = column[Option[String]]("street_address")
      def city = column[Option[String]]("city")
      def state = column[Option[String]]("state")
      def zip = column[Option[String]]("zip")
      def country = column[Option[String]]("country")
      def * =
        (propertyId, dataSource, bathrooms, bedrooms, listPrice, livingArea, propertyType, yearBuilt, lastUpdated, streetAddress, city, state, zip, country) <> (PropertyListing.tupled, PropertyListing.unapply)
    }

    val source: Source[PropertyListing, NotUsed] =
      Slick
        .source(TableQuery[PropertyListings].sortBy(_.propertyId).drop(offset).take(limit).result)

    val producerConfig = system.settings.config.getConfig("akkafka.producer.with-brokers")
    val producerSettings =
      ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

    source
      .map{ property =>
        val prodRec = new ProducerRecord[String, String](
          topic, property.propertyId.toString, property.toJson.compactPrint
        )
        println(s"[POSTRES] >>> Producer msg: $prodRec")  // For testing only
        prodRec
      }
      .runWith(Producer.plainSink(producerSettings))
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    import alpakkafka.model.PropertyListingJsonProtocol._
    implicit val slickSession = SlickSession.forConfig("slick-postgres")

    val topic = "property-listing-topic"
    val offset: Int = if (args.length >= 1) Try(args(0).toInt).getOrElse(0) else 0
    val limit: Int = if (args.length == 2) Try(args(1).toInt).getOrElse(Int.MaxValue) else Int.MaxValue

    runPropertyListing(topic, offset, limit) onComplete { _ =>
      slickSession.close()
      system.terminate()
    }
  }
  // sbt "runMain alpakkafka.producer.PostgresPlain [offset [limit]]"
  // e.g. sbt "runMain alpakkafka.producer.PostgresPlain 0 50"
}
