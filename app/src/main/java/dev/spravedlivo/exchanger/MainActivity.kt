package dev.spravedlivo.exchanger

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spravedlivo.exchanger.ui.theme.ExchangerTheme
import dev.spravedlivo.exchanger.viewmodel.MainScreenViewModel


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            ExchangerApp(this)
        }
    }
}

fun favouritesKey(from: String, to: String): String {
    return "${from}_${to}"
}

fun fromFavouritesKey(key: String): Pair<String, String> {
    val split = key.split("_")
    return split[0] to split[1]
}

fun favouritesKeyToReadable(key: String?): String? {
    return key?.replace("_", " → ")
}

@Composable
fun ExchangerApp(context: Context) {
    ExchangerTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val viewModel = viewModel<MainScreenViewModel>()
            val state by viewModel.state.collectAsState()
            if (!state.loaded) {
                Text(text = "Loading rates")
                val exchangeOptions = mutableListOf<String>()
                LaunchedEffect(Unit) {
                    val received : Map<String, String>? = ExchangeApi.getRates()
                    if (received == null) {
                        Toast.makeText(context, "Unable to fetch rates", Toast.LENGTH_LONG).show()
                        return@LaunchedEffect
                    }
                    received.entries.map { exchangeOptions.add("${it.key} (${it.value})") }
                    viewModel.updateLoaded(true, exchangeOptions, Settings.readStringSetKey(context, "favourites", setOf())!!)
                }
            }
            else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                        EditableDropdownMenu(
                            options = state.exchangeOptions,
                            selectedOptionText = state.exchangeFrom,
                            updateSelectedOptionText = { viewModel.updateExchangeFrom(it) }
                        )
                        Button(onClick = { viewModel.swapExchanges() }) {
                            Text(text = "Swap")
                        }
                        EditableDropdownMenu(
                            options = state.exchangeOptions,
                            selectedOptionText = state.exchangeTo,
                            updateSelectedOptionText = { viewModel.updateExchangeTo(it) }
                        )

                        Image(painterResource(id = (if (state.favourites.contains(favouritesKey(state.exchangeFrom, state.exchangeTo))) R.drawable.star_svgrepo_com_full else R.drawable.star_svgrepo_com))
                            , contentDescription = "",
                            modifier = Modifier.clickable {
                                viewModel.updateFavourites(context, favouritesKey(state.exchangeFrom, state.exchangeTo))
                            })
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableDropdownMenu(options: List<String>, selectedOptionText: String, updateSelectedOptionText: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded}) {
        TextField(
            value = selectedOptionText,
            onValueChange = { updateSelectedOptionText(it) },
            label = { Text("Label") },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },

            modifier = Modifier
                .menuAnchor()
                .width(150.dp),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { selectionOption ->
                if (selectedOptionText in selectionOption) {
                    DropdownMenuItem( { Text(text = selectionOption)},
                        onClick = {
                            updateSelectedOptionText(selectionOption.split(" ")[0])
                            expanded = false
                        }
                    )
                }

            }
        }
    }
}