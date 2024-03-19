package dev.spravedlivo.exchanger.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.spravedlivo.exchanger.ExchangeApi
import dev.spravedlivo.exchanger.ExchangerWidget
import dev.spravedlivo.exchanger.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenState(val exchangeFrom: String, val exchangeTo: String, val loaded: Boolean = false, val exchangeOptions: List<String> = listOf(), val favourites: Set<String> = setOf(), val error: String? = null)

class MainScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainScreenState("usd", "rub"))
    val state = _state.asStateFlow()
    fun updateExchangeFrom(exchangeFrom: String) {
        _state.update { currentState ->
            currentState.copy(exchangeFrom = exchangeFrom)
        }
    }

    fun loadRates(context: Context) {
        viewModelScope.launch {
            val received : Map<String, String>? = ExchangeApi.getRates()
            val exchangeOptions = mutableListOf<String>()
            if (received == null) {
                _state.update { currentState ->
                    currentState.copy(loaded = true, error = "Unable to fetch rates")
                }
                return@launch
            }
            received.entries.map { exchangeOptions.add("${it.key} (${it.value})") }
            _state.update { currentState ->
                currentState.copy(loaded = true, exchangeOptions = exchangeOptions, favourites = Settings.readStringSetKey(context, "favourites", setOf())!!)
            }
        }

    }
    fun updateFavourites(context: Context, key: String) {
        _state.update { currentState ->
            val new = if (currentState.favourites.contains(key)) currentState.favourites.minus(key) else currentState.favourites.plus(key)
            viewModelScope.launch {
                Settings.saveStringSetKey(context, "favourites", new)
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(ExchangerWidget::class.java)
                println(ids)
                ids.forEach {
                    updateAppWidgetState(context, it) { prefs ->
                        prefs[stringSetPreferencesKey("favourites")] = new
                    }
                    ExchangerWidget.update(context, it)
                }
            }
            currentState.copy(favourites = new)
        }


    }
    fun updateExchangeTo(exchangeTo: String) {
        _state.update { currentState ->
            currentState.copy(exchangeTo = exchangeTo)
        }
    }
    fun swapExchanges() {
        _state.update { currentState ->
            currentState.copy(exchangeFrom = currentState.exchangeTo, exchangeTo = currentState.exchangeFrom)
        }
    }
}