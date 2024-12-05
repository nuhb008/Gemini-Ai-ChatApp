package com.example.chatgptapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etQuestion = findViewById<EditText>(R.id.etQuestion)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val txtResponse = findViewById<TextView>(R.id.txtResponse)

        btnSubmit.setOnClickListener {
            val question = etQuestion.text.toString()

            if (question.isBlank()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Display user input as a toast for confirmation
            Toast.makeText(this, "Fetching response for: $question", Toast.LENGTH_SHORT).show()

            // Make the API call and handle response
            getResponse(question) { response ->
                runOnUiThread {
                    txtResponse.text = response
                }
            }
        }
    }

    private fun getResponse(question: String, callback: (String) -> Unit) {
        val apiKey = "AIzaSyAq7iWiYf3tXjZFO4VO7W1WYWKwFCavfMM"  // Replace with your actual API key
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

        // Build the JSON request body similar to your JavaScript example
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", question)  // Using the input question as the text
                        })
                    })
                })
            })
        }.toString()

        // Build the HTTP request
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        // Execute the API request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Error", "Failed to connect to API", e)
                callback("Failed to connect to API. Please try again later.")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                // Log the full response for debugging
                Log.v("data", body ?: "Empty response body")

                if (body != null) {
                    try {
                        val jsonObject = JSONObject(body)

                        // Check if the response contains the expected "candidates" array
                        val candidatesArray: JSONArray = jsonObject.optJSONArray("candidates") ?: JSONArray()

                        if (candidatesArray.length() > 0) {
                            // Access the "content" -> "parts" -> "text"
                            val content = candidatesArray.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts") ?: JSONArray()
                            val textResult = if (parts.length() > 0) {
                                parts.getJSONObject(0).optString("text", "No response text found")
                            } else {
                                "No parts found in the response."
                            }
                            callback(textResult)
                        } else {
                            callback("No candidates found in the response.")
                        }

                    } catch (e: Exception) {
                        Log.e("Parse Error", "Error parsing JSON response", e)
                        callback("Error parsing the server response.")
                    }
                } else {
                    callback("Empty response body.")
                }

                // Close the response body to avoid resource leaks
                response.body?.close()
            }
        })
    }
}
