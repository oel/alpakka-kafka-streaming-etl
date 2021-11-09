package alpakkafka.model

import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat

case class PropertyListing(
    propertyId: Int,
    dataSource: Option[String],
    bathrooms: Option[Double],
    bedrooms: Option[Int],
    listPrice: Option[Double],
    livingArea: Option[Int],
    propertyType: Option[String],
    yearBuilt: Option[String],
    lastUpdated: Option[String],
    streetAddress: Option[String],
    city: Option[String],
    state: Option[String],
    zip: Option[String],
    country: Option[String]
  ) {
    def summary(): String = {
      val ba = bathrooms.getOrElse(0)
      val br = bedrooms.getOrElse(0)
      val price = listPrice.getOrElse(0)
      val area = livingArea.getOrElse(0)
      val street = streetAddress.getOrElse("")
      val cit = city.getOrElse("")
      val sta = state.getOrElse("")
      s"PropertyID: $propertyId | Price: $$$price ${br}BR/${ba}BA/${area}sqft | Address: $street, $cit, $sta"
    }
  }

object PropertyListingJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonFormat: JsonFormat[PropertyListing] = jsonFormat14(PropertyListing.apply)
}

case class PartitionedRatedPropertyListing(
    partitionKey: String,
    ratingAffordability: Option[Int],
    ratingComfort: Option[Int],
    ratingNeighborhood: Option[Int],
    ratingSchools: Option[Int],
    propertyListing: PropertyListing
  )
