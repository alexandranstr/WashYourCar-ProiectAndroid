package com.example.washyourcar.data.network

object RetrofitClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/alexandranstr/spalatorie-api/refs/heads/main/"

    val apiService: ApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}