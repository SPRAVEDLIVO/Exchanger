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
import androidx.compose.ui.graphics.ColorFilter
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
            var loading by remember { mutableStateOf(false) }
            if (!loading) {
                loading = true
                viewModel.loadRates(context)
            }
            if (!state.loaded) {
                Text(text = "Loading rates")
            }
            else if (state.error != null) {
                Text(text = state.error!!)
            }
            else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EditableDropdownMenu(
                            options = state.exchangeOptions,
                            selectedOptionText = state.exchangeFrom,
                            updateSelectedOptionText = { viewModel.updateExchangeFrom(it) },
                            labelText = "From"
                        )
                        Button(onClick = { viewModel.swapExchanges() }) {
                            Text(text = "Swap")
                        }
                        EditableDropdownMenu(
                            options = state.exchangeOptions,
                            selectedOptionText = state.exchangeTo,
                            updateSelectedOptionText = { viewModel.updateExchangeTo(it) },
                            labelText = "To"
                        )

                        Image(painterResource(id = (if (state.favourites.contains(favouritesKey(state.exchangeFrom, state.exchangeTo))) R.drawable.star_svgrepo_com_full else R.drawable.star_svgrepo_com))
                            , contentDescription = "",
                            modifier = Modifier.clickable {
                                viewModel.updateFavourites(context, favouritesKey(state.exchangeFrom, state.exchangeTo))
                            }, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground))
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableDropdownMenu(options: List<String>, selectedOptionText: String, updateSelectedOptionText: (String) -> Unit, labelText: String) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded}) {
        TextField(
            value = selectedOptionText,
            onValueChange = { updateSelectedOptionText(it) },
            label = { Text(labelText) },
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
            // TODO add conversion
        }
    }
}