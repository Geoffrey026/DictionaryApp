package com.example.dictionaryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Jika tema khusus belum ada, Anda bisa menggunakan MaterialTheme
            MaterialTheme {
                DictionaryApp()
            }
        }
    }
}

@Composable
fun DictionaryApp() {
    var word by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("Enter a word to get its definition.") }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Input field for the word
                TextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Enter a word") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()  // Hide keyboard after search
                        coroutineScope.launch {
                            definition = withContext(Dispatchers.IO) {
                                fetchDefinition(word)
                            }
                        }
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Button
                Button(
                    onClick = {
                        keyboardController?.hide()  // Hide keyboard after button click
                        coroutineScope.launch {
                            definition = withContext(Dispatchers.IO) {
                                fetchDefinition(word)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Search")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result Text
                Text(
                    text = definition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    )
}

suspend fun fetchDefinition(word: String): String {
    if (word.isBlank()) return "Please enter a word."

    val apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
    val client = OkHttpClient()
    val request = Request.Builder().url(apiUrl).build()

    return try {
        Log.d("DictionaryApp", "Fetching definition for word: $word")
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() // Read the response body once
        Log.d("DictionaryApp", "API Response: $responseBody")  // Log the full response body

        if (!response.isSuccessful) {
            return "Word not found. (${response.code})"
        }

        if (responseBody.isNullOrBlank()) {
            return "No response from server."
        }

        parseDefinition(responseBody)
    } catch (e: Exception) {
        Log.e("DictionaryApp", "Error fetching definition", e) // Log the exception
        "Error fetching definition: ${e.message ?: "Unknown error"}"
    }
}

fun parseDefinition(jsonData: String): String {
    return try {
        val jsonArray = JSONArray(jsonData)
        if (jsonArray.length() == 0) return "No definition found."

        val wordObject = jsonArray.getJSONObject(0)
        val word = wordObject.getString("word")
        val meanings = wordObject.getJSONArray("meanings")
        val firstMeaning = meanings.getJSONObject(0)
        val definitions = firstMeaning.getJSONArray("definitions")
        if (definitions.length() == 0) return "No definitions available."
        val firstDefinition = definitions.getJSONObject(0).getString("definition")
        "$word: $firstDefinition"
    } catch (e: Exception) {
        "Error parsing definition: ${e.localizedMessage}"
    }
}

@Composable
@Preview
fun PreviewDictionaryApp() {
    // Jika tema belum dibuat, gunakan tema default MaterialTheme
    MaterialTheme {
        DictionaryApp()
    }
}
