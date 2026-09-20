package com.revisepdf.app.data.llm

// Lite flavour: no llama.cpp, so no on-device model. Callers already surface the failure from
// ensureLoaded as a message, and the AI UI is hidden behind BuildConfig.AI_ENABLED, so this is
// only reached if something tries to generate anyway.
class LlamaEngine {

    suspend fun ensureLoaded(modelPath: String, mmprojPath: String?): Unit =
        throw UnsupportedOperationException(
            "This is the lite build, which has no on-device AI. Install the full APK to generate questions.",
        )

    suspend fun generate(systemPrompt: String, userPrompt: String, maxTokens: Int = 160): String =
        throw UnsupportedOperationException("This build has no on-device AI.")

    suspend fun generateFromImage(
        systemPrompt: String,
        userPrompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        maxTokens: Int = 160,
    ): String = throw UnsupportedOperationException("This build has no on-device AI.")

    suspend fun close() = Unit
}
