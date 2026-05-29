package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ----------------------------------------------------
// 1. Moshi Models for Gemini Content Generation
// ----------------------------------------------------

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

// ----------------------------------------------------
// 2. Retrofit Endpoint Definition
// ----------------------------------------------------

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// ----------------------------------------------------
// 3. Client Singleton Network Configuration
// ----------------------------------------------------

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Configure 60-second timeouts to robustly support Gemini generation times
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// ----------------------------------------------------
// 4. Content Producer Helper Methods
// ----------------------------------------------------

object GeminiHelper {
    
    // Check if the API key is fallback placeholder
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "API_KEY_MISSING_NOTICE: Gemini API key is missing or is set to placeholder in secrets. Please configure GEMINI_API_KEY in the Secrets panel of AI Studio to activate intelligent features!"
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(
                BuildConfig.GEMINI_API_KEY,
                request
            )
            val responseText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            responseText ?: "Exception: Empty response received from the AI model."
        } catch (e: Exception) {
            "Error calling Gemini API: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun askGeminiWithImage(prompt: String, mimeType: String, base64Data: String): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "API_KEY_MISSING_NOTICE: Gemini API key is missing or set to placeholder. Please configure GEMINI_API_KEY in the Secrets panel of AI Studio."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Data))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(
                BuildConfig.GEMINI_API_KEY,
                request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Exception: Empty response received."
        } catch (e: Exception) {
            "Error calling Gemini API: ${e.localizedMessage ?: e.message}"
        }
    }
}
