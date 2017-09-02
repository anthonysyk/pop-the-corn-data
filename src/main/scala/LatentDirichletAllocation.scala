import breeze.numerics.log
import com.quantifind.charts.Highcharts._
import edu.stanford.nlp.process.Morphology
import models.FullMovie
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.clustering.{LDA, LDAModel}
import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.json4s.jackson.Json

import scala.collection.mutable

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

  val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("LatentDirichletAllocation")
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
      .filterNot(VocabularyUtils.getStopwords.contains)
      .filterNot(VocabularyUtils.getFirstNames.contains)
    )

    // Création d'un vecteur de wordcount
    val vocab: Map[String, Int] = dictionnaryBroadcast.value.zipWithIndex.toMap

    val documents: RDD[(Long, linalg.Vector)] = bagsOfWords.zipWithIndex().map {
      case (tokens, id) =>
        val _counter = new mutable.HashMap[Int, Double]()
        tokens.foreach(term => if (vocab.contains(term)) {
          val idx: Int = vocab(term)
          _counter(idx) = _counter.getOrElse(idx, 0.0) + 1.0
        })
        id -> Vectors.sparse(vocab.size, _counter.toSeq)
    }

    val lda = new LDA().setK(8).setMaxIterations(100)
    val ldaModel: LDAModel = lda.run(documents)

    val ldaResult: Array[(Array[Int], Array[Double])] = ldaModel.describeTopics(maxTermsPerTopic = 10)

    val test: Vector[TopicLDA] = ldaResult.zipWithIndex.map {
      case ((terms, termWeights), id) =>
        TopicLDA(
          id = (id + 1).toString,
          terms.zip(termWeights).map {
            case (termId, score) =>
              Term(termId.toString, dictionnaryBroadcast.value(termId), score)
          }
        )
    }.toVector

    test.foreach(println)

    test.reverse.foreach { topic =>
      histogram(topic.terms.map(term => term.label -> term.score))
      legend(s"Topic ${topic.id}" +: Nil)
    }

  }

}

