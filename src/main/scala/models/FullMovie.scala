package models

case class MovieStats(
                       color: String,
                       contentRating: Option[String],
                       faceNumbersInPoster: Option[Int],
                       aspectRatio: String,
                       castTotalFacebookLikes: Option[Int],
                       plotKeywords: Seq[String],
                       casting: Seq[Casting],
                       rating: Rating
                     )

case class FullMovie(
                      id: Option[Int],
                      adult: Option[Boolean],
                      budget: Option[Double],
                      genres: Seq[String],
                      imdb_id: Option[String],
                      original_language: Option[String],
                      original_title: Option[String],
                      overview: Option[String],
                      popularity: Option[Double],
                      poster_path: Option[String],
                      production_companies: Seq[String],
                      production_countries: Seq[String],
                      release_date: Option[String],
                      revenue: Option[Int],
                      runtime: Option[Int],
                      spoken_languages: Seq[String],
                      status: Option[String],
                      title: Option[String],
                      vote_average: Option[Double],
                      vote_count: Option[Int],
                      movieStats: MovieStats
                    )

case class Rating(
                   score: Option[Double],
                   numberOfReviews: Option[Int],
                   numberOfVotes: Option[Int],
                   numberOfCriticForReviews: Option[Int]
                 )

case class Casting(role: Option[String], name: Option[String], facebookLikes: Int)