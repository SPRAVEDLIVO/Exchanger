package dev.spravedlivo.exchanger

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.firstOrNull


class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExchangerWidget
}


object ExchangerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        updateAppWidgetState(context, id) { prefs ->
            val data = context.dataStore.data.firstOrNull() ?: return@updateAppWidgetState
            prefs[favouritesKey] = data[favouritesKey] ?: setOf()
        }
        provideContent {
            WidgetContent(context, id)
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
    fun WidgetContent(context: Context, id: GlanceId) {
        val textStyle = TextStyle(color = ColorProvider(MaterialTheme.colorScheme.inversePrimary))
        val text = currentState(textKey)
        val favourites = currentState(favouritesKey)
        val selectedExchange = currentState(selectedExchangeKey)
        val exchanged = currentState(exchangedKey)
        if (favourites == null) LaunchedEffect(Unit) {
            ExchangerWidget.update(context, id)
        }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(favouritesKeyToReadable(selectedExchange) ?: "Nothing selected", modifier =
                GlanceModifier.clickable(actionStartActivity<MainActivity>()), style = textStyle)
            favourites?.forEach {
                Button(text = favouritesKeyToReadable(it)!!,
                        onClick = actionRunCallback(
                            ChooseExchange.javaClass,
                            actionParametersOf(
                                ActionParameters.Key<String>("currency") to it
                            )))
            }
            val disabled = selectedExchange == null || text.isNullOrBlank()
            val colors = if (disabled)
                ButtonDefaults.buttonColors(backgroundColor = GlanceTheme.colors.secondary)
            else ButtonDefaults.buttonColors()
            val action1 = if (disabled) actionRunCallback(Noop.javaClass) else actionRunCallback(ExchangeButtonAction.javaClass)
            val action2 = if (disabled) actionRunCallback(Noop.javaClass) else actionRunCallback(ReverseCallback.javaClass)
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Button(text = "go", onClick = action1, colors = colors)
                Button(text = "rev", onClick = action2, colors = colors)
            }
            Text(exchanged ?: "", style = textStyle)
            Text(text ?: "", style = textStyle)

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

object Noop: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {

    }
}

object ReverseCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val values = fromFavouritesKey(prefs[ExchangerWidget.selectedExchangeKey]!!)
            prefs[ExchangerWidget.selectedExchangeKey] = favouritesKey(values.second, values.first)
        }
        ExchangerWidget.update(context, glanceId)
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
        val amount = prefs[ExchangerWidget.textKey]
        if (read != null && amount != null) {
            val split = fromFavouritesKey(read)
            val need =
                ExchangeApi.getRate(split.first, split.second)
                    ?: return failure(context, glanceId)
            updateAppWidgetState(context, glanceId) { newPrefs ->
                newPrefs[ExchangerWidget.exchangedKey] = "%.2f".format((need * amount.toFloat()))
            }
            ExchangerWidget.update(context, glanceId)
        }
        else return failure(context, glanceId)

    }
    private suspend fun failure(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { newPrefs ->
            newPrefs[ExchangerWidget.exchangedKey] = "Error"
        }
        ExchangerWidget.update(context, glanceId)
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
