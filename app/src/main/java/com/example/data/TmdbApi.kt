package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TmdbResponse(
    val results: List<TmdbItem>?
)

@JsonClass(generateAdapter = true)
data class TmdbItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null,
    @Json(name = "media_type") val mediaType: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbDetailResponse(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    val genres: List<TmdbGenre>? = null,
    val runtime: Int? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    val credits: TmdbCredits? = null
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCredits(
    val cast: List<TmdbCast>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCast(
    val id: Int,
    val name: String,
    val character: String?,
    @Json(name = "profile_path") val profilePath: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideoResponse(
    val results: List<TmdbVideo>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideo(
    val id: String,
    val key: String,
    val site: String,
    val type: String
)

interface TmdbService {
    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String
    ): TmdbResponse

    @GET("tv/popular")
    suspend fun getPopularTvSeries(
        @Query("api_key") apiKey: String
    ): TmdbResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String
    ): TmdbResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_origin_country") originCountry: String? = null,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbResponse

    @GET("discover/tv")
    suspend fun discoverTvSeries(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genres: String? = null,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbResponse

    @GET("{media_type}/{id}")
    suspend fun getDetails(
        @Path("media_type") mediaType: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits"
    ): TmdbDetailResponse

    @GET("{media_type}/{id}/videos")
    suspend fun getVideos(
        @Path("media_type") mediaType: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbVideoResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbResponse

    @GET("search/tv")
    suspend fun searchTvSeries(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbResponse

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): TmdbService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(TmdbService::class.java)
        }
    }
}
