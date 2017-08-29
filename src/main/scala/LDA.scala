import models.FullMovie
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.json4s.jackson.Json

object LDA {

  import org.apache.log4j.{Level, Logger}

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("LDA")
  val ss: SparkSession = SparkSession.builder().config(conf).getOrCreate()

  lazy val index = "full_movie"
  lazy val typename = "movie"

  import org.elasticsearch.spark._


  def main(args: Array[String]): Unit = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    implicit val formats = DefaultFormats

    val movies = ss.sparkContext.esRDD(s"$index/$typename").values
      .map(mapping => parse(Json(DefaultFormats).write(mapping)))


  }

}

