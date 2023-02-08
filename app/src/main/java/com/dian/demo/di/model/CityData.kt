package com.dian.demo.di.model

data class ProvinceData(
    val name: String,
    val city: List<String>
)

data class CityData(
    val name: String,
    val area: List<String>
)