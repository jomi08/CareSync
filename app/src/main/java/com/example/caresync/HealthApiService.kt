package com.example.caresync

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface HealthApiService {

    // Wikipedia API — fetches health topic summaries
    // this API works on all emulators without any issues
    @GET("w/api.php")
    suspend fun getHealthInfo(
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "extracts|info",
        @Query("exintro") exintro: String = "true",
        @Query("explaintext") explaintext: String = "true",
        @Query("inprop") inprop: String = "url",
        @Query("titles") titles: String = "Nutrition|Exercise|Sleep|Diabetes|Cardiology|Mental_health",
        @Query("format") format: String = "json",
        @Query("origin") origin: String = "*"
    ): Response<WikiResponse>
}