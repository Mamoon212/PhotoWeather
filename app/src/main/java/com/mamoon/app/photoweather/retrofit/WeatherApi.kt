package com.mamoon.app.photoweather.retrofit

import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("data/2.5/weather/")
    fun getWeatherForCity(
        @Query("q") cityName: String, @Query("units") unit: String = "metric"
    ): Deferred<Result>
}