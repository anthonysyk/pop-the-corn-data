package nlp

import breeze.numerics.log
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

object TFIDF {
  def preprocessData(bagsOfWords: RDD[Array[String]], dictionnaryBroadcast: Broadcast[Array[String]]) = {

    val numberOfArticles = bagsOfWords.count

    val inverseDocumentFrequency: RDD[(String, Double)] = bagsOfWords
      .flatMap(article => article.filter(dictionnaryBroadcast.value.contains).distinct.map(_ -> 1))
      .reduceByKey(_ + _)
      .map {
        case (termLabel, numberOfArticlesContainingTerm) =>
          termLabel -> log(numberOfArticles / numberOfArticlesContainingTerm.toDouble)
      }.sortBy(_._2, false)
      .persist

    val termFrequencyByArticle: RDD[Seq[(String, Double)]] = bagsOfWords
      .map { article =>
        val numberOfWords = article.length
        article.filter(dictionnaryBroadcast.value.contains)
          .map(word => word -> 1)
          .groupBy(_._1)
          .mapValues(value => value.map(_._2).sum.toDouble / numberOfWords)
          .toSeq
      }
  }

}