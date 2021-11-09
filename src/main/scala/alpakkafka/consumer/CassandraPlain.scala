package alpakkafka.consumer

import alpakkafka.model.PropertyListing
import alpakkafka.query

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import akka.stream.alpakka.cassandra.scaladsl._
import akka.stream.alpakka.cassandra.{CassandraSessionSettings, CassandraWriteSettings}
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}

import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CassandraPlain {

  def runPropertyListing(consumerGroup: String,
                         topic: String)(implicit
                                        cassandraSession: CassandraSession,
                                        jsonFormat: JsonFormat[PropertyListing],
                                        system: ActorSystem,
                                        ec: ExecutionContext): Future[Done] = {

    val consumerConfig = system.settings.config.getConfig("akkafka.consumer.with-brokers")
    val consumerSettings =
      ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
        .withGroupId(consumerGroup)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val table = "propertydata.property_listing"
    val partitions = 10 // number of partitions

    val statementBinder: (ConsumerRecord[String, String], PreparedStatement) => BoundStatement = {
      case (msg, preparedStatement) =>
        val p = msg.value().parseJson.convertTo[PropertyListing]
        preparedStatement.bind(
          (p.propertyId % partitions).toString, Int.box(p.propertyId), p.dataSource.getOrElse("unknown"),
          Double.box(p.bathrooms.getOrElse(0)), Int.box(p.bedrooms.getOrElse(0)), Double.box(p.listPrice.getOrElse(0)), Int.box(p.livingArea.getOrElse(0)),
          p.propertyType.getOrElse(""), p.yearBuilt.getOrElse(""), p.lastUpdated.getOrElse(""), p.streetAddress.getOrElse(""), p.city.getOrElse(""), p.state.getOrElse(""), p.zip.getOrElse(""), p.country.getOrElse("")
        )
    }
    val cassandraFlow: Flow[ConsumerRecord[String, String], ConsumerRecord[String, String], NotUsed] =
      CassandraFlow.create(
        CassandraWriteSettings.defaults,
        s"""INSERT INTO $table (partition_key, property_id, data_source, bathrooms, bedrooms, list_price, living_area, property_type, year_built, last_updated, street_address, city, state, zip, country)
           |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin,
        statementBinder
      )

    val control: DrainingControl[Done] =
      Consumer
        .plainSource(consumerSettings, Subscriptions.topics(topic))
        .via(cassandraFlow)
        .toMat(Sink.ignore)(DrainingControl.apply)
        .run()

    Thread.sleep(2000)
    control.drainAndShutdown()
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    import alpakkafka.model.PropertyListingJsonProtocol._
    implicit val cassandraSession: CassandraSession =
      CassandraSessionRegistry.get(system).sessionFor(CassandraSessionSettings())

    val consumerGroup = "cassandra-property"
    val topic = "property-listing-topic"

    runPropertyListing(consumerGroup, topic) onComplete (println)

    Thread.sleep(6000)

    // Query all rows in table property_listing (Note: restrict to a partitionKey if the table is big!)
    query.CassandraPropertyListing.query() onComplete {
      case Success(res) => println(s">>> # of rows: ${res.size}")
      case Failure(e) => println(s"ERROR: $e")
    }

    Thread.sleep(2000)
    system.terminate()
  }
  // sbt "runMain alpakkafka.consumer.CassandraPlain"
}
