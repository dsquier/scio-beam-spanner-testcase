package com.joinhoney.sciobeamspannertestcase

import java.util

import cats.syntax.either._
import com.google.cloud.spanner.Mutation
import com.google.datastore.v1.{Entity, Value}
import com.spotify.scio.ContextAndArgs
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.parse
import org.apache.beam.sdk.io.gcp.spanner.{MutationGroup, SpannerConfig, SpannerIO}
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

case class InputRecord(
  created: Long,
  expires: Long,
  image_url: String,
  merch_id: String,
  notify_at_price: Double,
  notify_threshold: Double,
  original_price: Double,
  price_notified: List[Notification],
  shop_url: String,
  store_id: String,
  title: String,
  updated: Long,
  user_email: String,
  user_id: String,
  variant_id: String,
  product_id: String)

case class Location(
  region: String,
  city: String,
  country: String)

case class Notification(
  price: Double,
  seller_id: String,
  time: Long,
  location: Location)

object Testcase {
  val logger = LoggerFactory.getLogger(this.getClass)
  val projectId = "<YOUR-PROJECT-ID>"
  val spannerInstanceId = "sciobeamspannertestcase"
  val spannerDatabaseId = "testcase"
  val spannerParentTable = "parent_table"
  val spannerChildTable = "child_table"

  def main(cmdlineArgs: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(cmdlineArgs)

    val inputFolder = "gs://spotify-scio-testcase/input-1.json"

    val spannerConfig = SpannerConfig.create()
      .withProjectId(projectId)
      .withInstanceId(spannerInstanceId)
      .withDatabaseId(spannerDatabaseId)

    val spannerOutput = SpannerIO.write()
      .withProjectId(projectId)
      .withInstanceId(spannerInstanceId)
      .withDatabaseId(spannerDatabaseId)
      .withBatchSizeBytes(1024)
      .grouped()

    // Execute pipeline
    sc.withName("Read from Storage").textFile(inputFolder)
      .withName("Parse Entity").map(parseEntity)
      .withName("Create Object").map(toDroplist)
      .withName("Create Spanner Mutations").map(toSpannerMutations)
      .saveAsCustomOutput("Save to Spanner", spannerOutput)

    val res = sc.close

    res.waitUntilFinish()
  }

  def generateUuid: String = java.util.UUID.randomUUID().toString

  def dollarsToCents(dollars: Double): Int = (dollars * 100).toInt

  def parseEntity(entity: String): Option[Json] = {
    parse(entity) match {
      case Left(error) => None
      case Right(result) => Some(result)
    }
  }

  def toDroplist(json: Option[Json]): InputRecord = {
    val doc = json.get
    val test_list = List(Notification(0.00, "0", 1500000000, Location("", "", "")))
    val test_bool = false
    val test_string = java.util.UUID.randomUUID().toString
    val test_long: Long = 1500000000
    val test_double: Double = 1.23

    InputRecord(
      test_long,
      test_long,
      test_string,
      test_string,
      test_double,
      test_double,
      test_double,
      doc.hcursor.downField("price_notified").as[List[Notification]].getOrElse(test_list),
      test_string,
      test_string,
      test_string,
      test_long,
      test_string,
      test_string,
      test_string,
      test_string)
  }

  def toSpannerMutations(item: InputRecord): MutationGroup = {
    val droplistMutation = Mutation.newInsertOrUpdateBuilder(spannerParentTable)
      .set("col02").to(item.user_id)
      .set("col03").to(DigestUtils.md5Hex(item.user_id))
      .set("col01").to(item.product_id)
      .set("col04").to(item.created)
      .set("col05").to(item.expires)
      .set("col06").to(dollarsToCents(item.original_price))
      .set("col07").to(dollarsToCents(item.notify_at_price))
      .set("col08").to(item.updated)
      .build()

    val notifications = item.price_notified

    // Check for droplistNotifications
    notifications.length match {
      case x if x > 0 =>
        val droplistNotificationsMutations = new util.ArrayList[Mutation]()

        notifications.foreach(notification => {
          droplistNotificationsMutations.add(
            Mutation.newInsertOrUpdateBuilder(spannerChildTable)
              .set("col03").to(generateUuid)
              .set("col04").to(DigestUtils.md5Hex(item.user_id))
              .set("col02").to(item.product_id)
              .set("col11").to(dollarsToCents(item.original_price))
              .set("col10").to(dollarsToCents(item.notify_at_price))
              .set("col07").to(item.created)
              .set("col01").to(notification.time)
              .set("col08").to(generateUuid)
              .set("col06").to(notification.location.country)
              .set("col12").to(notification.location.region)
              .set("col05").to(notification.location.city)
              .build())
        })

        MutationGroup.create(droplistMutation, droplistNotificationsMutations)
      case _ => MutationGroup.create(droplistMutation)
    }
  }
}

class GetDroplistFromEntityDoFn()
  extends DoFn[Entity, InputRecord] {

  @ProcessElement
  def processElement(context: ProcessContext) {
    val entity = context.element
    val price_notified_array = entity.getPropertiesMap.get("price_notified").getArrayValue
    val price_notified_count = price_notified_array.getValuesCount
    var notifications = new ListBuffer[Notification]()
    val price_notified_values_list = price_notified_array.getValuesList

    if (price_notified_count > 0) {
      price_notified_values_list
        .iterator().asScala
        .foreach(item => notifications += getPriceNotified(item.getEntityValue))
    }

    val test_bool = false
    val test_string = java.util.UUID.randomUUID().toString
    val test_long: Long = 1500000000
    val test_double: Double = 1.23

    context.output(
      InputRecord(
        test_long,
        test_long,
        test_string,
        test_string,
        test_double,
        test_double,
        test_double,
        notifications.toList,
        test_string,
        test_string,
        test_string,
        test_long,
        test_string,
        test_string,
        test_string,
        test_string)
    )
  }

  def getPriceNotified(element: Entity): Notification = {
    val time = element.getPropertiesMap.get("time").getIntegerValue
    val price = element.getPropertiesMap.get("price").getStringValue.toDouble
    val sellerId = filterNullString(element, "sellerId")

    if (element.containsProperties("location")) {
      val location = element.getPropertiesMap.get("location").getEntityValue
      Notification(
        price,
        sellerId,
        time,
        Location(
          filterNullString(location, "region"),
          filterNullString(location, "city"),
          filterNullString(location, "country")
        )
      )
    } else {
      Notification(price, sellerId, time, Location("", "", ""))
    }
  }

  def filterNullString(entity: Entity, string: String): String = {
    if (entity.containsProperties(string)) {
      entity.getPropertiesMap.get(string).getStringValue
    } else {
      ""
    }
  }
}
