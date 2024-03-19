package dev.spravedlivo.exchanger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ExchangeApi {
    const val API_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api"
    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
        }
    }
    suspend fun getRate(from: String, to: String): Float? {
        val response = client.get("$API_URL@latest/v1/currencies/${from}.json")
        if (!response.status.isSuccess()) return null
        return response.body<String>().trim().replace("\n", "").replace(" ", "").split("${to}\":")[1].split(",")[0].replace("}", "").toFloat()
    }
    suspend fun getRates(): Map<String, String>? {
        val response = client.get("$API_URL@latest/v1/currencies.json")
        if (!response.status.isSuccess()) return null
        return response.body()
    }

}