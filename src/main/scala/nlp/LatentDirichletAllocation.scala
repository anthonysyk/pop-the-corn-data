package nlp

import edu.stanford.nlp.process.Morphology
import models.FullMovie
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.apache.spark.mllib.clustering.{DistributedLDAModel, LDA, LDAModel}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.json4s.jackson.Json

case class Term(
                 id: String,
                 label: String,
                 score: Double
               )

case class TopicLDA(
                     id: String,
                     terms: Seq[Term]
                   )

object LatentDirichletAllocation {

  import org.apache.log4j.{Level, Logger}

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("LatentDirichletAllocation")
  implicit val ss: SparkSession = SparkSession.builder().config(conf).getOrCreate()

  lazy val index = "full_movie"
  lazy val typename = "movie"

  import org.elasticsearch.spark._

  def main(args: Array[String]): Unit = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    val movieRDD: RDD[FullMovie] = ss.sparkContext.esRDD(s"$index/$typename").values
      .map { mapping =>
        implicit val formats = DefaultFormats
        parse(Json(DefaultFormats).write(mapping)).extract[FullMovie]
      }


    // Découpage des documents en sacs de mots
    val bagsOfWords: RDD[Array[String]] = movieRDD.map(_.overview).filter(_.nonEmpty).map(_.getOrElse(""))
      .map { overview =>
        overview.toLowerCase.split("\\s")
          .filter(_.length > 3)
          .filter(_.forall(_.isLetter))
      }.persist()


    // Création d'un vocabulaire trié par nombre d'occurence (popularité) dans l'ensemble de nos documents
    val dictionnaryBroadcast: Broadcast[Array[String]] = ss.sparkContext.broadcast(bagsOfWords
      .flatMap(_.map{ word =>
        val NlpHelper = new Morphology()
        NlpHelper.stem(word) -> 1})
      .reduceByKey(_ + _)
      .keys.collect()
      .filterNot(NLPHelper.getStopwords.contains)
      .filterNot(NLPHelper.getFirstNames.contains)
    )

    // Création d'un vecteur de wordcount
    val vocab: Map[String, Int] = dictionnaryBroadcast.value.zipWithIndex.toMap

    val documents = bagsOfWords.zipWithIndex().map {
      case (tokens, id) =>
        val counter: Map[Int, Double] = tokens.filter(vocab.contains)
          .map { term =>
            vocab(term) -> 1.0
          }.toMap
        id -> Vectors.sparse(vocab.size, counter.toSeq)
    }
    val lda = new LDA().setK(8).setMaxIterations(100)
    val ldaModel: LDAModel = lda.run(documents)

    val ldaResult: Array[(Array[Int], Array[Double])] = ldaModel.describeTopics(maxTermsPerTopic = 10)

    val test: Seq[TopicLDA] = ldaResult.zipWithIndex.map {
      case ((terms, termWeights), id) =>
        TopicLDA(
          id = (id + 1).toString,
          terms.zip(termWeights).map {
            case (termId, score) =>
              Term(termId.toString, dictionnaryBroadcast.value(termId), score)
          }
        )
    }.toSeq


    // HighChart
    import com.quantifind.charts.Highcharts._
    test.reverse.foreach { topic =>
      histogram(topic.terms.map(term => term.label -> term.score))
      legend(s"Topic ${topic.id}" +: Nil)
    }

    // Doesn't work
//    ldaModel.save(ss.sparkContext, "src/main/resources/lda-model")

  }

}

object UseLDA {

  import org.apache.log4j.{Level, Logger}

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("LatentDirichletAllocation")
  implicit val ss: SparkSession = SparkSession.builder().config(conf).getOrCreate()

  def main(args: Array[String]): Unit = {
    DistributedLDAModel.load(ss.sparkContext, "src/main/resources/lda-model")
      .topicDistributions
      .foreach(println)
  }

}

