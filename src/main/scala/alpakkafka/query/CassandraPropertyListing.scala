package alpakkafka.query

import alpakkafka.model.PropertyListing

import akka.actor.ActorSystem
import akka.stream.scaladsl._

import akka.stream.alpakka.cassandra.scaladsl._
import akka.stream.alpakka.cassandra.CassandraSessionSettings

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object CassandraPropertyListing {

  def query(partitionKey: String = "",
            limit: Int = Int.MaxValue)(implicit
                            cassandraSession: CassandraSession,
                            system: ActorSystem,
                            ec: ExecutionContext): Future[Seq[(String, PropertyListing)]] = {

    val table = "propertydata.property_listing"
    val whereClause = if (partitionKey.isEmpty) "" else s"WHERE partition_key = '$partitionKey'"
    val limitExpr = if (limit == Int.MaxValue) "" else s"LIMIT $limit"
    val cql = s"SELECT * FROM $table $whereClause $limitExpr;"

    CassandraSource(cql)
      .map{ r =>
        val partitionKey = r.getString("partition_key")
        val propertyListingTuple = (
          r.getInt("property_id"),
          Option(r.getString("data_source")),
          Option(r.getDouble("list_price")),
          Option(r.getInt("bedrooms")),
          Option(r.getDouble("bathrooms")),
          Option(r.getInt("living_area")),
          Option(r.getString("property_type")),
          Option(r.getString("year_built")),
          Option(r.getString("last_updated")),
          Option(r.getString("street_address")),
          Option(r.getString("city")),
          Option(r.getString("state")),
          Option(r.getString("zip")),
          Option(r.getString("country"))
        )
        val propertyListing = PropertyListing.tupled(propertyListingTuple)
        (partitionKey, propertyListing)
      }
      .runWith(Sink.seq)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    implicit val cassandraSession: CassandraSession =
      CassandraSessionRegistry.get(system).sessionFor(CassandraSessionSettings())

    val partitionKey: String = if (args.length >= 1) args(0) else ""
    val limit: Int = if (args.length == 2) Try(args(1).toInt).getOrElse(Int.MaxValue) else Int.MaxValue

    query(partitionKey, limit) onComplete {
      case Success(res) =>
        res.foreach(println)
        println(s">>> # of rows: ${res.size}")
      case Failure(e) =>
        println(s"ERROR: $e")
    }

    Thread.sleep(5000)
    system.terminate()
  }
  // sbt "runMain alpakkafka.query.CassandraPropertyListing [partitionKey [limit]]"
  // e.g. sbt "runMain alpakkafka.query.CassandraPropertyListing 0 50"
}
