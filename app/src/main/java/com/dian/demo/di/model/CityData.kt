package com.dian.demo.di.model

import org.json.JSONObject

data class CityData(
    private val name: String,
    private val next: JSONObject?
){
    fun getName(): String {
        return name
    }

    fun getNext(): JSONObject? {
        return next
    }
}
