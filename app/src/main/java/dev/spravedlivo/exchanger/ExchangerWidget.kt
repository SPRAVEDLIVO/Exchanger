package dev.spravedlivo.exchanger

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.ktor.client.call.body
import io.ktor.client.request.get


class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExchangerWidget
}


object ExchangerWidget : GlanceAppWidget() {
    // TODO initial
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            WidgetContent(context)
        }
    }
    private fun actionGenerator(text: String): Action {
        return actionRunCallback(TypeCallback.javaClass, actionParametersOf(ActionParameters.Key<String>("updateText") to text))
    }
    val textKey = stringPreferencesKey("text")
    val selectedExchangeKey = stringPreferencesKey("selectedExchange")
    private val favouritesKey = stringSetPreferencesKey("favourites")
    val exchangedKey = stringPreferencesKey("exchanged")

    @Composable
    @GlanceComposable
    fun WidgetContent(context: Context) {

        val text = currentState(textKey)
        val favourites = currentState(favouritesKey)
        val selectedExchange = currentState(selectedExchangeKey)
        val exchanged = currentState(exchangedKey)

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            favourites?.forEach {

                Button(text = it.replace("_", " → "),
                        onClick = actionRunCallback(
                            ChooseExchange.javaClass,
                            actionParametersOf(
                                ActionParameters.Key<String>("currency") to it
                            )))
            }
            if (selectedExchange != null && !text.isNullOrBlank()) {
                Button(text = "go", onClick = actionRunCallback(ExchangeButtonAction.javaClass))
            }
            if (exchanged != null) Text(exchanged, style = TextStyle(color = ColorProvider(MaterialTheme.colorScheme.inversePrimary)))
            if (text != null) Text(text, style = TextStyle(color = ColorProvider(MaterialTheme.colorScheme.inversePrimary)))

            Column {
                var count = 1
                for (row in 1..3) {
                    Row {
                        for (column in 1..3) {
                            val resolved = when (row) {
                                1 -> 6+count
                                2 -> count
                                3 -> (row*column) / 3
                                else -> 0
                            }
                            Button(text = resolved.toString(), onClick = actionGenerator(resolved.toString()))
                            count += 1
                        }
                    }
                }
                Row {
                    Button("⬅", onClick = actionGenerator("back"))
                    Button(text = "0", onClick = actionGenerator("0"))
                    Button(text = ".", onClick = actionGenerator("."))
                }
            }


        }
    }
}

object ExchangeButtonAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs: MutablePreferences = ExchangerWidget.getAppWidgetState(glanceId=glanceId, context = context)
        val read = prefs[ExchangerWidget.selectedExchangeKey]
        var amount = prefs[ExchangerWidget.textKey]
        println(read)
        println(amount)
        if (read != null && amount != null) {
            val split = read.split("_")
            if (amount.endsWith(".")) amount = amount.substring(0, read.length)
            val rates = client.get("${API_URL}/latest/currencies/${split[0]}/${split[1]}.json").body<String>().trim().replace("\n", "").replace(" ", "")
            val need = rates.split("${split[1]}\":")[1].replace("}", "")
            println(rates)
            updateAppWidgetState(context, glanceId) { newPrefs ->
                println(need!!.toFloat() * amount.toFloat())
                newPrefs[ExchangerWidget.exchangedKey] = if (need != null) (need.toFloat() * amount.toFloat()).toString() else "Error"
            }
            ExchangerWidget.update(context, glanceId)
        }

    }

}


object ChooseExchange : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val got = parameters[ActionParameters.Key("currency")] as String
        updateAppWidgetState(context, glanceId) {prefs ->
            prefs[ExchangerWidget.selectedExchangeKey] = got
        }
        ExchangerWidget.update(context, glanceId)
    }
}

object TypeCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val got = parameters[ActionParameters.Key("updateText")] as String
            val hold = prefs[ExchangerWidget.textKey]
            when (got) {
                "back" -> if (hold != null) prefs[ExchangerWidget.textKey] = hold.substring(0, hold.length-1)
                "." -> if (hold == null) prefs[ExchangerWidget.textKey] = "0."
                    else prefs[ExchangerWidget.textKey] = hold.replace(".", "") + got
                else -> if (hold != null) prefs[ExchangerWidget.textKey] = hold + got else prefs[ExchangerWidget.textKey] = got
            }
        }
        ExchangerWidget.update(context, glanceId)
    }
}
