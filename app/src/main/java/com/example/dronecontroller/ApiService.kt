package com.example.dronecontroller

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

var BASE_URL = "http://192.168.4.1"

interface ApiService {
    @GET("/motor")
    suspend fun setMotorPower(@Query("power") level: Int)
    @GET("/mpu")
    suspend fun getMpuAcceleration():Response<Acceleration>
}

data class Acceleration(val xAc: Float = 0f, val yAc: Float = 0f, val zAc: Float = 0f)
data class ResultInResponse(val result:String)
object RetrofitInstance {
    //    private
    val api: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}