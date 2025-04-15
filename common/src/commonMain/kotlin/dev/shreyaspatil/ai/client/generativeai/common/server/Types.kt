/*
 * Copyright 2024 Shreyas Patil
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
package dev.shreyaspatil.ai.client.generativeai.common.server

import dev.shreyaspatil.ai.client.generativeai.common.shared.Content
import dev.shreyaspatil.ai.client.generativeai.common.shared.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.common.util.SerializableEnum
import dev.shreyaspatil.ai.client.generativeai.common.util.enumSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

object BlockReasonSerializer :
    KSerializer<BlockReason> by enumSerializer(BlockReason.entries)

object HarmProbabilitySerializer :
    KSerializer<HarmProbability> by enumSerializer(HarmProbability.entries)

object FinishReasonSerializer :
    KSerializer<FinishReason> by enumSerializer(FinishReason.entries)

@Serializable
data class PromptFeedback(
    val blockReason: BlockReason? = null,
    val safetyRatings: List<SafetyRating>? = null,
)

@Serializable(BlockReasonSerializer::class)
enum class BlockReason(override val serialName: String) : SerializableEnum<BlockReason> {
    UNKNOWN("UNKNOWN"),
    UNSPECIFIED("BLOCKED_REASON_UNSPECIFIED"),
    SAFETY("SAFETY"),
    OTHER("OTHER"),
}

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: FinishReason? = null,
    val safetyRatings: List<SafetyRating>? = null,
    val citationMetadata: CitationMetadata? = null,
    val groundingMetadata: GroundingMetadata? = null,
)

@Serializable
data class CitationMetadata
@OptIn(ExperimentalSerializationApi::class)
constructor(
    @JsonNames("citations") val citationSources: List<CitationSources>,
)

@Serializable
data class CitationSources(
    val startIndex: Int = 0,
    val endIndex: Int,
    val uri: String,
    val license: String? = null,
)

@Serializable
data class SafetyRating(
    val category: HarmCategory,
    val probability: HarmProbability,
    // TODO(): any reason not to default to false?
    val blocked: Boolean? = null,
    val probabilityScore: Float? = null,
    val severity: HarmSeverity? = null,
    val severityScore: Float? = null,
)

@Serializable
data class GroundingMetadata(
    @SerialName("web_search_queries") val webSearchQueries: List<String>?,
    @SerialName("search_entry_point") val searchEntryPoint: SearchEntryPoint?,
    @SerialName("retrieval_queries") val retrievalQueries: List<String>?,
    @SerialName("grounding_attribution") val groundingAttribution: List<GroundingAttribution>?,
)

@Serializable
data class SearchEntryPoint(
    @SerialName("rendered_content") val renderedContent: String?,
    @SerialName("sdk_blob") val sdkBlob: String?,
)

@Serializable
data class GroundingAttribution(
    val segment: Segment,
    @SerialName("confidence_score") val confidenceScore: Float?,
)

@Serializable
data class Segment(
    @SerialName("start_index") val startIndex: Int,
    @SerialName("end_index") val endIndex: Int,
)

@Serializable(HarmProbabilitySerializer::class)
enum class HarmProbability(override val serialName: String) : SerializableEnum<HarmProbability> {
    UNKNOWN("UNKNOWN"),
    UNSPECIFIED("HARM_PROBABILITY_UNSPECIFIED"),
    NEGLIGIBLE("NEGLIGIBLE"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
}

@Serializable
enum class HarmSeverity {
    UNKNOWN,

    @SerialName("HARM_SEVERITY_UNSPECIFIED")
    UNSPECIFIED,

    @SerialName("HARM_SEVERITY_NEGLIGIBLE")
    NEGLIGIBLE,

    @SerialName("HARM_SEVERITY_LOW")
    LOW,

    @SerialName("HARM_SEVERITY_MEDIUM")
    MEDIUM,

    @SerialName("HARM_SEVERITY_HIGH")
    HIGH,
}

@Serializable(FinishReasonSerializer::class)
enum class FinishReason(override val serialName: String) : SerializableEnum<FinishReason> {
    UNKNOWN("UNKNOWN"),
    UNSPECIFIED("FINISH_REASON_UNSPECIFIED"),
    STOP("STOP"),
    MAX_TOKENS("MAX_TOKENS"),
    SAFETY("SAFETY"),
    RECITATION("RECITATION"),
    OTHER("OTHER"),
}

@Serializable
data class GRpcError(val code: Int, val message: String, val details: List<GRpcErrorDetails> = emptyList())

@Serializable data class GRpcErrorDetails(val reason: String? = null)
