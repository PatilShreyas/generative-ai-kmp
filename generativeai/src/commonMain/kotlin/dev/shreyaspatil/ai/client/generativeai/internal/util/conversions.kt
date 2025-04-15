/*
 * Copyright 2023 Shreyas Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.shreyaspatil.ai.client.generativeai.internal.util

import dev.shreyaspatil.ai.client.generativeai.common.CountTokensResponse
import dev.shreyaspatil.ai.client.generativeai.common.GenerateContentResponse
import dev.shreyaspatil.ai.client.generativeai.common.RequestOptions
import dev.shreyaspatil.ai.client.generativeai.common.client.GenerationConfig
import dev.shreyaspatil.ai.client.generativeai.common.client.Schema
import dev.shreyaspatil.ai.client.generativeai.common.server.BlockReason
import dev.shreyaspatil.ai.client.generativeai.common.server.Candidate
import dev.shreyaspatil.ai.client.generativeai.common.server.CitationSources
import dev.shreyaspatil.ai.client.generativeai.common.server.FinishReason
import dev.shreyaspatil.ai.client.generativeai.common.server.HarmProbability
import dev.shreyaspatil.ai.client.generativeai.common.server.PromptFeedback
import dev.shreyaspatil.ai.client.generativeai.common.server.SafetyRating
import dev.shreyaspatil.ai.client.generativeai.common.shared.Blob
import dev.shreyaspatil.ai.client.generativeai.common.shared.BlobPart
import dev.shreyaspatil.ai.client.generativeai.common.shared.CodeExecutionResult
import dev.shreyaspatil.ai.client.generativeai.common.shared.Content
import dev.shreyaspatil.ai.client.generativeai.common.shared.ExecutableCode
import dev.shreyaspatil.ai.client.generativeai.common.shared.ExecutableCodePart
import dev.shreyaspatil.ai.client.generativeai.common.shared.FileData
import dev.shreyaspatil.ai.client.generativeai.common.shared.FileDataPart
import dev.shreyaspatil.ai.client.generativeai.common.shared.FunctionCall
import dev.shreyaspatil.ai.client.generativeai.common.shared.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.common.shared.FunctionResponse
import dev.shreyaspatil.ai.client.generativeai.common.shared.FunctionResponsePart
import dev.shreyaspatil.ai.client.generativeai.common.shared.HarmBlockThreshold
import dev.shreyaspatil.ai.client.generativeai.common.shared.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.common.shared.Outcome
import dev.shreyaspatil.ai.client.generativeai.common.shared.Part
import dev.shreyaspatil.ai.client.generativeai.common.shared.SafetySetting
import dev.shreyaspatil.ai.client.generativeai.common.shared.TextPart
import dev.shreyaspatil.ai.client.generativeai.type.Bitmap
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold
import dev.shreyaspatil.ai.client.generativeai.type.CitationMetadata
import dev.shreyaspatil.ai.client.generativeai.type.CodeExecutionResultPart
import dev.shreyaspatil.ai.client.generativeai.type.ExecutionOutcome
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallingConfig
import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.ImagePart
import dev.shreyaspatil.ai.client.generativeai.type.SerializationException
import dev.shreyaspatil.ai.client.generativeai.type.Tool
import dev.shreyaspatil.ai.client.generativeai.type.ToolConfig
import dev.shreyaspatil.ai.client.generativeai.type.UsageMetadata
import dev.shreyaspatil.ai.client.generativeai.type.content
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal fun dev.shreyaspatil.ai.client.generativeai.type.RequestOptions.toInternal() = RequestOptions(timeout, apiVersion)

internal fun dev.shreyaspatil.ai.client.generativeai.type.Content.toInternal() = Content(this.role, this.parts.map { it.toInternal() })

@OptIn(ExperimentalEncodingApi::class)
internal fun dev.shreyaspatil.ai.client.generativeai.type.Part.toInternal(): Part = when (this) {
    is dev.shreyaspatil.ai.client.generativeai.type.TextPart -> TextPart(text)
    is ImagePart -> BlobPart(Blob("image/png", image.asBase64()))
    is dev.shreyaspatil.ai.client.generativeai.type.BlobPart ->
        BlobPart(Blob(mimeType, Base64.Mime.encode(blob)))

    is dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart ->
        FunctionCallPart(FunctionCall(name, args.orEmpty()))

    is dev.shreyaspatil.ai.client.generativeai.type.FunctionResponsePart ->
        FunctionResponsePart(FunctionResponse(name, response))

    is dev.shreyaspatil.ai.client.generativeai.type.FileDataPart ->
        FileDataPart(FileData(fileUri = uri, mimeType = mimeType))
    is dev.shreyaspatil.ai.client.generativeai.type.ExecutableCodePart ->
        ExecutableCodePart(ExecutableCode(language, code))
    is CodeExecutionResultPart ->
        dev.shreyaspatil.ai.client.generativeai.common.shared.CodeExecutionResultPart(
            CodeExecutionResult(outcome.toInternal(), output),
        )
    else ->
        throw SerializationException(
            "The given subclass of Part ($this) is not supported in the serialization yet.",
        )
}

internal fun dev.shreyaspatil.ai.client.generativeai.type.SafetySetting.toInternal() = SafetySetting(harmCategory.toInternal(), threshold.toInternal())

internal fun dev.shreyaspatil.ai.client.generativeai.type.GenerationConfig.toInternal() = GenerationConfig(
    temperature = temperature,
    topP = topP,
    topK = topK,
    candidateCount = candidateCount,
    maxOutputTokens = maxOutputTokens,
    stopSequences = stopSequences,
    responseMimeType = responseMimeType,
    responseSchema = responseSchema?.toInternal(),
)

internal fun dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.toInternal() = when (this) {
    dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.HARASSMENT -> HarmCategory.HARASSMENT
    dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.HATE_SPEECH -> HarmCategory.HATE_SPEECH
    dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.SEXUALLY_EXPLICIT ->
        HarmCategory.SEXUALLY_EXPLICIT

    dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.DANGEROUS_CONTENT ->
        HarmCategory.DANGEROUS_CONTENT

    dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.UNKNOWN -> HarmCategory.UNKNOWN
}

internal fun BlockThreshold.toInternal() = when (this) {
    BlockThreshold.NONE -> HarmBlockThreshold.BLOCK_NONE
    BlockThreshold.ONLY_HIGH -> HarmBlockThreshold.BLOCK_ONLY_HIGH
    BlockThreshold.MEDIUM_AND_ABOVE -> HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    BlockThreshold.LOW_AND_ABOVE -> HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
    BlockThreshold.UNSPECIFIED -> HarmBlockThreshold.UNSPECIFIED
}

internal fun ExecutionOutcome.toInternal() = when (this) {
    ExecutionOutcome.UNSPECIFIED -> Outcome.UNSPECIFIED
    ExecutionOutcome.OK -> Outcome.OUTCOME_OK
    ExecutionOutcome.FAILED -> Outcome.OUTCOME_FAILED
    ExecutionOutcome.DEADLINE_EXCEEDED -> Outcome.OUTCOME_DEADLINE_EXCEEDED
}

internal fun Tool.toInternal() = dev.shreyaspatil.ai.client.generativeai.common.client.Tool(
    functionDeclarations?.map { it.toInternal() },
    codeExecution = codeExecution,
)

internal fun ToolConfig.toInternal() = dev.shreyaspatil.ai.client.generativeai.common.client.ToolConfig(
    dev.shreyaspatil.ai.client.generativeai.common.client.FunctionCallingConfig(
        when (functionCallingConfig.mode) {
            FunctionCallingConfig.Mode.ANY ->
                dev.shreyaspatil.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.ANY

            FunctionCallingConfig.Mode.AUTO ->
                dev.shreyaspatil.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.AUTO

            FunctionCallingConfig.Mode.NONE ->
                dev.shreyaspatil.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.NONE
        },
    ),
)

internal fun dev.shreyaspatil.ai.client.generativeai.common.UsageMetadata.toPublic(): UsageMetadata = UsageMetadata(promptTokenCount ?: 0, candidatesTokenCount ?: 0, totalTokenCount ?: 0)

internal fun FunctionDeclaration.toInternal() = dev.shreyaspatil.ai.client.generativeai.common.client.FunctionDeclaration(
    name,
    description,
    Schema(
        properties = parameters.associate { it.name to it.toInternal() },
        required = requiredParameters,
        type = "OBJECT",
        nullable = false,
    ),
)

internal fun <T> dev.shreyaspatil.ai.client.generativeai.type.Schema<T>.toInternal(): Schema = Schema(
    type.name,
    description,
    format,
    nullable,
    enum,
    properties?.mapValues { it.value.toInternal() },
    required,
    items?.toInternal(),
)

internal fun Candidate.toPublic(): dev.shreyaspatil.ai.client.generativeai.type.Candidate {
    val safetyRatings = safetyRatings?.map { it.toPublic() }.orEmpty()
    val citations = citationMetadata?.citationSources?.map { it.toPublic() }.orEmpty()
    val finishReason = finishReason.toPublic()

    return dev.shreyaspatil.ai.client.generativeai.type.Candidate(
        this.content?.toPublic() ?: content("model") {},
        safetyRatings,
        citations,
        finishReason,
    )
}

internal fun Content.toPublic(): dev.shreyaspatil.ai.client.generativeai.type.Content = dev.shreyaspatil.ai.client.generativeai.type.Content(role, parts.map { it.toPublic() })

@OptIn(ExperimentalEncodingApi::class)
internal fun Part.toPublic(): dev.shreyaspatil.ai.client.generativeai.type.Part = when (this) {
    is TextPart -> dev.shreyaspatil.ai.client.generativeai.type.TextPart(text)
    is BlobPart -> {
        val data = Base64.decode(inlineData.data)
        if (inlineData.mimeType.contains("image")) {
            ImagePart(Bitmap(data))
        } else {
            dev.shreyaspatil.ai.client.generativeai.type.BlobPart(inlineData.mimeType, data)
        }
    }

    is FunctionCallPart ->
        dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart(
            functionCall.name,
            functionCall.args.orEmpty(),
        )

    is FunctionResponsePart ->
        dev.shreyaspatil.ai.client.generativeai.type.FunctionResponsePart(
            functionResponse.name,
            functionResponse.response,
        )

    is FileDataPart ->
        dev.shreyaspatil.ai.client.generativeai.type.FileDataPart(
            fileData.fileUri,
            fileData.mimeType,
        )
    is dev.shreyaspatil.ai.client.generativeai.common.shared.CodeExecutionResultPart ->
        CodeExecutionResultPart(
            codeExecutionResult.outcome.toPublic(),
            codeExecutionResult.output,
        )
    else ->
        throw SerializationException(
            "Unsupported part type \"${this}\" provided. This model may not be supported by this SDK.",
        )
}

internal fun CitationSources.toPublic() = CitationMetadata(startIndex = startIndex, endIndex = endIndex, uri = uri ?: "", license = license)

internal fun SafetyRating.toPublic() = dev.shreyaspatil.ai.client.generativeai.type.SafetyRating(
    category.toPublic(),
    probability.toPublic(),
)

internal fun PromptFeedback.toPublic(): dev.shreyaspatil.ai.client.generativeai.type.PromptFeedback {
    val safetyRatings = safetyRatings?.map { it.toPublic() }.orEmpty()
    return dev.shreyaspatil.ai.client.generativeai.type.PromptFeedback(
        blockReason?.toPublic(),
        safetyRatings,
    )
}

internal fun FinishReason?.toPublic() = when (this) {
    null -> null
    FinishReason.MAX_TOKENS -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.MAX_TOKENS
    FinishReason.RECITATION -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.RECITATION
    FinishReason.SAFETY -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.SAFETY
    FinishReason.STOP -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.STOP
    FinishReason.OTHER -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.OTHER
    FinishReason.UNSPECIFIED -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.UNSPECIFIED
    FinishReason.UNKNOWN -> dev.shreyaspatil.ai.client.generativeai.type.FinishReason.UNKNOWN
}

internal fun HarmCategory.toPublic() = when (this) {
    HarmCategory.HARASSMENT -> dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.HARASSMENT
    HarmCategory.HATE_SPEECH -> dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.HATE_SPEECH
    HarmCategory.SEXUALLY_EXPLICIT ->
        dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.SEXUALLY_EXPLICIT

    HarmCategory.DANGEROUS_CONTENT ->
        dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.DANGEROUS_CONTENT
    HarmCategory.UNKNOWN -> dev.shreyaspatil.ai.client.generativeai.type.HarmCategory.UNKNOWN
}

internal fun HarmProbability.toPublic() = when (this) {
    HarmProbability.HIGH -> dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.HIGH
    HarmProbability.MEDIUM -> dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.MEDIUM
    HarmProbability.LOW -> dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.LOW
    HarmProbability.NEGLIGIBLE -> dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.NEGLIGIBLE
    HarmProbability.UNSPECIFIED ->
        dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.UNSPECIFIED

    HarmProbability.UNKNOWN -> dev.shreyaspatil.ai.client.generativeai.type.HarmProbability.UNKNOWN
}

internal fun BlockReason.toPublic() = when (this) {
    BlockReason.UNSPECIFIED -> dev.shreyaspatil.ai.client.generativeai.type.BlockReason.UNSPECIFIED
    BlockReason.SAFETY -> dev.shreyaspatil.ai.client.generativeai.type.BlockReason.SAFETY
    BlockReason.OTHER -> dev.shreyaspatil.ai.client.generativeai.type.BlockReason.OTHER
    BlockReason.UNKNOWN -> dev.shreyaspatil.ai.client.generativeai.type.BlockReason.UNKNOWN
}

internal fun Outcome.toPublic() = when (this) {
    Outcome.UNSPECIFIED -> ExecutionOutcome.UNSPECIFIED
    Outcome.OUTCOME_OK -> ExecutionOutcome.OK
    Outcome.OUTCOME_FAILED -> ExecutionOutcome.FAILED
    Outcome.OUTCOME_DEADLINE_EXCEEDED -> ExecutionOutcome.DEADLINE_EXCEEDED
}

internal fun GenerateContentResponse.toPublic() = dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse(
    candidates?.map { it.toPublic() }.orEmpty(),
    promptFeedback?.toPublic(),
    usageMetadata?.toPublic(),
)

internal fun CountTokensResponse.toPublic() = dev.shreyaspatil.ai.client.generativeai.type.CountTokensResponse(totalTokens)
