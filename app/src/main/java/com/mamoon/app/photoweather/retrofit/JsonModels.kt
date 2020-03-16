package com.mamoon.app.photoweather.retrofit

data class Result(
    val weather: List<Weather>,
    val main: Main
)

data class Weather(
    val description: String
)

data class Main(
    val temp: Float
)