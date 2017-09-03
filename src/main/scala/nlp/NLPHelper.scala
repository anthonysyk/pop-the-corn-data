package nlp

object NLPHelper {

  import edu.stanford.nlp.process.Morphology
  import org.apache.spark.sql.SparkSession

  def getStopwords(implicit ss: SparkSession) = {
    import ss.implicits._
    ss.read.option("header", "true").csv("src/main/resources/stopwords.csv").map { row =>
      row.getAs[String]("stopwords")
    }.collect().toVector
  }

  def getFirstNames(implicit ss: SparkSession) = {
    import ss.implicits._
    ss.read.option("header", "true").csv("src/main/resources/firstnames.csv").map { row =>
      row.getAs[String]("firstname").toLowerCase()
    }.rdd.filter(_.length > 2).distinct.collect.toVector
  }

  def main(args: Array[String]): Unit = {
    val m = new Morphology()

    val test = "Hello it is a test where have you been organizing ?".split(" ")

    test.foreach(word => println(m.stem(word)))

  }
}
