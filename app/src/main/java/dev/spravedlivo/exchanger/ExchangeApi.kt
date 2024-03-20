package dev.spravedlivo.exchanger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ExchangeApi {
    const val API_URL1 = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/"
    const val API_URL2 = "https://latest.currency-api.pages.dev/v1/"
    private var useApiUrl: MutableMap<Int, Int> = mutableMapOf()

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 1000
        }
    }
    private suspend fun tryApiEndpoints(endpointId: Int, vararg endpoints: String): HttpResponse? {
        var response: HttpResponse? = null
        val endpointsMutable = mutableListOf<String>()
        val useApiUrlIndex = useApiUrl[endpointId]
        useApiUrlIndex != null && endpointsMutable.add(endpoints[useApiUrlIndex])
        endpointsMutable.addAll(endpoints.filterIndexed { index, _ -> index != useApiUrlIndex })
        for ((index, endpoint) in endpointsMutable.withIndex()) {
            try {
                response = client.get(endpoint)
                if (response.status.isSuccess()) {
                    useApiUrl[endpointId] = index
                    return response
                }
            }
            catch (_: HttpRequestTimeoutException) {

            }
        }
        return response
    }
    suspend fun getRate(from: String, to: String): Float? {
        //val response = client.get("$API_URL@latest/v1/currencies/${from}.json")
        val response = tryApiEndpoints(0,
            API_URL1 + "currencies/${from}.min.json",
            API_URL2 + "currencies/${from}.min.json",
            API_URL1 + "currencies/${from}.json",
            API_URL2 + "currencies/${from}.json")
        if (response == null || !response.status.isSuccess()) return null
        return response.body<String>().trim().replace("\n", "").replace(" ", "").split("${to}\":")[1].split(",")[0].replace("}", "").toFloat()
    }
    suspend fun getRates(): Map<String, String>? {
        //val response = client.get("$API_URL@latest/v1/currencies.json")
        val response = tryApiEndpoints(1,
            API_URL1 + "currencies.min.json",
            API_URL2 + "currencies.min.json",
            API_URL1 + "currencies.json",
            API_URL2 + "currencies.json")
        if (response == null || !response.status.isSuccess()) return null
        return response.body()
    }

}