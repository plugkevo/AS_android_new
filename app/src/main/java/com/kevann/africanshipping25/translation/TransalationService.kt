package com.kevann.africanshipping25.translation

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface TranslationService {
    @POST("language/translate/v2")
    suspend fun translateText(
        @Query("key") apiKey: String,
        @Body request: TranslationRequest
    ): TranslationResponse
}

data class TranslationRequest(
    val q: List<String>,
    val target: String,
    val source: String = "en"
)

data class TranslationResponse(
    val data: TranslationData
)

data class TranslationData(
    val translations: List<Translation>
)

data class Translation(
    val translatedText: String,
    val detectedSourceLanguage: String?
)