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
package dev.shreyaspatil.ai.client.generativeai.common

import dev.shreyaspatil.ai.client.generativeai.common.server.BlockReason
import dev.shreyaspatil.ai.client.generativeai.common.server.FinishReason
import dev.shreyaspatil.ai.client.generativeai.common.server.HarmProbability
import dev.shreyaspatil.ai.client.generativeai.common.server.HarmSeverity
import dev.shreyaspatil.ai.client.generativeai.common.shared.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.common.util.goldenUnaryFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class UnarySnapshotTests {
    private val testTimeout = 5.seconds

    @Test
    fun `short reply`() =
        goldenUnaryFile("success-basic-reply-short.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.finishReason shouldBe FinishReason.STOP
                response.candidates?.first()?.content?.parts?.isEmpty() shouldBe false
                response.candidates?.first()?.safetyRatings?.isEmpty() shouldBe false
            }
        }

    @Test
    fun `long reply`() =
        goldenUnaryFile("success-basic-reply-long.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.finishReason shouldBe FinishReason.STOP
                response.candidates?.first()?.content?.parts?.isEmpty() shouldBe false
                response.candidates?.first()?.safetyRatings?.isEmpty() shouldBe false
            }
        }

    @Test
    fun `unknown enum`() =
        goldenUnaryFile("success-unknown-enum.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.first {
                    it.safetyRatings?.any { it.category == HarmCategory.UNKNOWN } ?: false
                }
            }
        }

    @Test
    fun `safetyRatings including severity`() =
        goldenUnaryFile("success-including-severity.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.safetyRatings?.isEmpty() shouldBe false
                response.candidates?.first()?.safetyRatings?.all {
                    it.probability == HarmProbability.NEGLIGIBLE
                } shouldBe true
                response.candidates?.first()?.safetyRatings?.all { it.probabilityScore != null } shouldBe
                    true
                response.candidates?.first()?.safetyRatings?.all {
                    it.severity == HarmSeverity.NEGLIGIBLE
                } shouldBe true
                response.candidates?.first()?.safetyRatings?.all { it.severityScore != null } shouldBe true
            }
        }

    @Test
    fun `prompt blocked for safety`() =
        goldenUnaryFile("failure-prompt-blocked-safety.json") {
            withTimeout(testTimeout) {
                shouldThrow<PromptBlockedException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                } should { it.response.promptFeedback?.blockReason shouldBe BlockReason.SAFETY }
            }
        }

    @Test
    fun `empty content`() =
        goldenUnaryFile("failure-empty-content.json") {
            withTimeout(testTimeout) {
                shouldThrow<SerializationException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `http error`() =
        goldenUnaryFile("failure-http-error.json", HttpStatusCode.PreconditionFailed) {
            withTimeout(testTimeout) {
                shouldThrow<ServerException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `user location error`() =
        goldenUnaryFile("failure-unsupported-user-location.json", HttpStatusCode.PreconditionFailed) {
            withTimeout(testTimeout) {
                shouldThrow<UnsupportedUserLocationException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `stopped for safety`() =
        goldenUnaryFile("failure-finish-reason-safety.json") {
            withTimeout(testTimeout) {
                val exception =
                    shouldThrow<ResponseStoppedException> {
                        apiController.generateContent(textGenerateContentRequest("prompt"))
                    }
                exception.response.candidates?.first()?.finishReason shouldBe FinishReason.SAFETY
            }
        }

    @Test
    fun `citation returns correctly`() =
        goldenUnaryFile("success-citations.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.citationMetadata?.citationSources?.isNotEmpty() shouldBe true
            }
        }

    @Test
    fun `citation returns correctly with missing license and startIndex`() =
        goldenUnaryFile("success-citations-nolicense.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.citationMetadata?.citationSources?.isNotEmpty() shouldBe true
                // Verify the values in the citation source
                with(response.candidates?.first()?.citationMetadata?.citationSources?.first()!!) {
                    license shouldBe null
                    startIndex shouldBe 0
                }
            }
        }

    @Test
    fun `response includes usage metadata`() =
        goldenUnaryFile("success-usage-metadata.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.finishReason shouldBe FinishReason.STOP
                response.usageMetadata shouldNotBe null
                response.usageMetadata?.totalTokenCount shouldBe 363
            }
        }

    @Test
    fun `response includes partial usage metadata`() =
        goldenUnaryFile("success-partial-usage-metadata.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.finishReason shouldBe FinishReason.STOP
                response.usageMetadata shouldNotBe null
                response.usageMetadata?.promptTokenCount shouldBe 6
                response.usageMetadata?.totalTokenCount shouldBe null
            }
        }

    @Test
    fun `citation returns correctly when using alternative name`() =
        goldenUnaryFile("success-citations-altname.json") {
            withTimeout(testTimeout) {
                val response = apiController.generateContent(textGenerateContentRequest("prompt"))

                response.candidates?.isEmpty() shouldBe false
                response.candidates?.first()?.citationMetadata?.citationSources?.isNotEmpty() shouldBe true
            }
        }

    @Test
    fun `invalid response`() =
        goldenUnaryFile("failure-invalid-response.json") {
            withTimeout(testTimeout) {
                shouldThrow<SerializationException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `malformed content`() =
        goldenUnaryFile("failure-malformed-content.json") {
            withTimeout(testTimeout) {
                shouldThrow<SerializationException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `invalid api key`() =
        goldenUnaryFile("failure-api-key.json", HttpStatusCode.BadRequest) {
            withTimeout(testTimeout) {
                shouldThrow<InvalidAPIKeyException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `quota exceeded`() =
        goldenUnaryFile("failure-quota-exceeded.json", HttpStatusCode.BadRequest) {
            withTimeout(testTimeout) {
                shouldThrow<QuotaExceededException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `image rejected`() =
        goldenUnaryFile("failure-image-rejected.json", HttpStatusCode.BadRequest) {
            withTimeout(testTimeout) {
                shouldThrow<ServerException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }

    @Test
    fun `unknown model`() =
        goldenUnaryFile("failure-unknown-model.json", HttpStatusCode.NotFound) {
            withTimeout(testTimeout) {
                shouldThrow<ServerException> {
                    apiController.generateContent(textGenerateContentRequest("prompt"))
                }
            }
        }
}