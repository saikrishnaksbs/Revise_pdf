package com.revisepdf.app.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ladenthin.llama.LlamaModel
import net.ladenthin.llama.parameters.InferenceParameters
import net.ladenthin.llama.parameters.ModelParameters
import net.ladenthin.llama.value.ChatMessage
import net.ladenthin.llama.value.ContentPart

// One native model is loaded at a time and it is not safe to decode concurrently, so every
// call funnels through a mutex. Loading costs seconds and gigabytes, so the model is kept
// open between generations and only released via close().
class LlamaEngine {

    private val mutex = Mutex()
    private var model: LlamaModel? = null
    private var loadedModelPath: String? = null
    private var loadedMmprojPath: String? = null

    suspend fun ensureLoaded(modelPath: String, mmprojPath: String?) = mutex.withLock {
        if (model != null && loadedModelPath == modelPath && loadedMmprojPath == mmprojPath) return@withLock
        withContext(Dispatchers.IO) {
            model?.close()
            val parameters = ModelParameters()
                .setModel(modelPath)
                .setCtxSize(CONTEXT_SIZE)
                .setThreads(THREADS)
                .setGpuLayers(0)
            if (mmprojPath != null) {
                parameters.setMmproj(mmprojPath).setDevices("none").setMmprojOffload(false)
            }
            model = LlamaModel(parameters)
            loadedModelPath = modelPath
            loadedMmprojPath = mmprojPath
        }
    }

    suspend fun generate(systemPrompt: String, userPrompt: String, maxTokens: Int = MAX_TOKENS): String =
        generate(systemPrompt, ChatMessage("user", userPrompt), maxTokens)

    suspend fun generateFromImage(
        systemPrompt: String,
        userPrompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        maxTokens: Int = MAX_TOKENS,
    ): String = generate(
        systemPrompt,
        ChatMessage("user", listOf(ContentPart.text(userPrompt), ContentPart.imageBytes(imageBytes, mimeType))),
        maxTokens,
    )

    private suspend fun generate(systemPrompt: String, message: ChatMessage, maxTokens: Int): String =
        mutex.withLock {
            val active = model ?: error("Model is not loaded")
            val parameters = InferenceParameters("")
                .withMessages(listOf(ChatMessage("system", systemPrompt), message))
                .withNPredict(maxTokens)
                .withTemperature(TEMPERATURE)
                .withTopK(TOP_K)
                .withTopP(TOP_P)
                .withRepeatPenalty(REPEAT_PENALTY)
                .withRepeatLastN(REPEAT_LAST_N)

            withContext(Dispatchers.IO) { active.chatCompleteText(parameters) }
        }

    suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) { model?.close() }
        model = null
        loadedModelPath = null
        loadedMmprojPath = null
    }

    private companion object {
        const val CONTEXT_SIZE = 4096
        const val THREADS = 4
        const val MAX_TOKENS = 160
        const val TEMPERATURE = 0.3f
        const val TOP_K = 40
        const val TOP_P = 0.9f
        const val REPEAT_PENALTY = 1.1f
        const val REPEAT_LAST_N = 64
    }
}
