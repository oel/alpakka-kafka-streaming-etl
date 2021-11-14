package alpakkafka.producer

import alpakkafka.model.PropertyListing

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Keep, Source}
import akka.{Done, NotUsed}

import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import spray.json._

import java.nio.file.Paths
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object CsvPlain {

  def toClassPropertyListing(csvMap: Map[String, String]): PropertyListing = {
    val propertyListingTuple = (
      csvMap("property_id").toInt, // primary key (let it crash in case of bad data)
      Option(csvMap("data_source")),
      Option(Try(csvMap("bathrooms").toDouble).getOrElse(0.0)),
      Option(Try(csvMap("bedrooms").toInt).getOrElse(0)),
      Option(Try(csvMap("list_price").toDouble).getOrElse(0.0)),
      Option(Try(csvMap("living_area").toInt).getOrElse(0)),
      Option(csvMap("property_type")),
      Option(csvMap("year_built")),
      Option(csvMap("last_updated")),
      Option(csvMap("street_address")),
      Option(csvMap("city")),
      Option(csvMap("state")),
      Option(csvMap("zip")),
      Option(csvMap("country"))
    )
    PropertyListing.tupled(propertyListingTuple)
  }

  def runPropertyListing(topic: String,
                         csvFilePath: String,
                         offset: Int = 0,
                         limit: Int = Int.MaxValue)(implicit
                                         jsonFormat: JsonFormat[PropertyListing],
                                         system: ActorSystem,
                                         ec: ExecutionContext): Future[Done] = {

    val source: Source[PropertyListing, NotUsed] =
      FileIO.fromPath(Paths.get(csvFilePath))
        .via(CsvParsing.lineScanner(CsvParsing.Tab))
        .viaMat(CsvToMap.toMapAsStrings())(Keep.right)
        .drop(offset).take(limit)
        .map(toClassPropertyListing(_))

    val producerConfig = system.settings.config.getConfig("akkafka.producer.with-brokers")
    val producerSettings =
      ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

    source
      .map{ property =>
        val prodRec = new ProducerRecord[String, String](
          topic, property.propertyId.toString, property.toJson.compactPrint
        )
        println(s"[CSV] >>> Producer msg: $prodRec")  // For testing only
        prodRec
      }
      .runWith(Producer.plainSink(producerSettings))
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    import alpakkafka.model.PropertyListingJsonProtocol._

    val topic = "property-listing-topic"
    val csvFilePath = "src/main/resources/property_listing_file_csv_500.tsv"
    val offset: Int = if (args.length >= 1) Try(args(0).toInt).getOrElse(1) else 1  // Header line excluded!
    val limit: Int = if (args.length == 2) Try(args(1).toInt).getOrElse(Int.MaxValue) else Int.MaxValue

    runPropertyListing(topic, csvFilePath, offset, limit) onComplete { _ => system.terminate() }
  }
  // sbt "runMain alpakkafka.producer.CsvPlain [offset [limit]]"
  // e.g. sbt "runMain alpakkafka.producer.CsvPlain 1 50"  # Header line excluded!
}
