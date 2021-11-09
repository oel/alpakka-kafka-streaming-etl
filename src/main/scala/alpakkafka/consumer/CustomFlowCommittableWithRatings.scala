package alpakkafka.consumer

import alpakkafka.model.{PropertyListing, PropertyRating}

import akka.actor.ActorSystem
import akka.Done

import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object CustomFlowCommittableWithRatings {

  def customBusinessLogic(key: String, value: String, rating: PropertyRating)(
    implicit ec: ExecutionContext): Future[Done] = Future {

    println(s"KEY: $key  VALUE: $value  RATING: $rating")
    // Perform custom business logic with key/value
    // and save to an external storage, etc.
    Done
  }

  def runPropertyListing(consumerGroup: String,
                         topic: String)(implicit
                                         jsonFormat: JsonFormat[PropertyListing],
                                         system: ActorSystem,
                                         ec: ExecutionContext): Future[Done] = {

    val consumerConfig = system.settings.config.getConfig("akkafka.consumer.with-brokers")
    val consumerSettings =
      ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
        .withGroupId(consumerGroup)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val committerConfig = system.settings.config.getConfig("akka.kafka.committer")
    val committerSettings = CommitterSettings(committerConfig)

    val control =
      Consumer
        .committableSource(consumerSettings, Subscriptions.topics(topic))
        .via(PropertyRating.compute())
        .mapAsync(1) { case (rating, msg) =>
          customBusinessLogic(msg.record.key, msg.record.value, rating)
            .map(_ => msg.committableOffset)
        }
        .toMat(Committer.sink(committerSettings))(DrainingControl.apply)
        .run()

    Thread.sleep(5000)
    control.drainAndShutdown()
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    import alpakkafka.model.PropertyListingJsonProtocol._

    val consumerGroup = "custom-flow-property-rating"
    val topic = "property-listing-topic"

    runPropertyListing(consumerGroup, topic) onComplete { _ => system.terminate() }
  }
  // sbt "runMain alpakkafka.consumer.CustomFlowCommittableWithRatings"
}
