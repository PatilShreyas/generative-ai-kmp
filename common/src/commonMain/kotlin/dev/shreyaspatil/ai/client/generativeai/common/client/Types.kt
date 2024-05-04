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
package dev.shreyaspatil.ai.client.generativeai.common.client

import dev.shreyaspatil.ai.client.generativeai.common.util.SerializableEnum
import dev.shreyaspatil.ai.client.generativeai.common.util.enumSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerationConfig(
    val temperature: Float?,
    @SerialName("top_p") val topP: Float?,
    @SerialName("top_k") val topK: Int?,
    @SerialName("candidate_count") val candidateCount: Int?,
    @SerialName("max_output_tokens") val maxOutputTokens: Int?,
    @SerialName("stop_sequences") val stopSequences: List<String>?,
)

@Serializable data class Tool(val functionDeclarations: List<FunctionDeclaration>)

@Serializable
data class ToolConfig(
    @SerialName("function_calling_config") val functionCallingConfig: FunctionCallingConfig,
)

@Serializable
data class FunctionCallingConfig(val mode: Mode) {
    @Serializable(ModeSerializer::class)
    enum class Mode(override val serialName: String) : SerializableEnum<Mode> {
        UNSPECIFIED("MODE_UNSPECIFIED"),
        AUTO("AUTO"),
        ANY("ANY"),
        NONE("NONE"),
    }
}

@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema,
)

@Serializable
data class Schema(
    val type: String,
    val description: String? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val items: Schema? = null,
)

internal object ModeSerializer :
    KSerializer<FunctionCallingConfig.Mode> by enumSerializer(FunctionCallingConfig.Mode.entries)
