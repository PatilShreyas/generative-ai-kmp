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

import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.TimeoutCancellationException

/** Parent class for any errors that occur. */
sealed class GoogleGenerativeAIException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    companion object {

        /**
         * Converts a [Throwable] to a [GoogleGenerativeAIException].
         *
         * Will populate default messages as expected, and propagate the provided [cause] through the
         * resulting exception.
         */
        fun from(cause: Throwable): GoogleGenerativeAIException =
            when (cause) {
                is GoogleGenerativeAIException -> cause
                is JsonConvertException,
                is kotlinx.serialization.SerializationException,
                ->
                    SerializationException(
                        "Something went wrong while trying to deserialize a response from the server.",
                        cause,
                    )
                is TimeoutCancellationException ->
                    RequestTimeoutException("The request failed to complete in the allotted time.")
                else -> UnknownException("Something unexpected happened.", cause)
            }
    }
}

/** Something went wrong while trying to deserialize a response from the server. */
class SerializationException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/** The server responded with a non 200 response code. */
class ServerException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/** The server responded that the API Key is no valid. */
class InvalidAPIKeyException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/**
 * A request was blocked for some reason.
 *
 * See the [response's][response] `promptFeedback.blockReason` for more information.
 *
 * @property response the full server response for the request.
 */
class PromptBlockedException(val response: GenerateContentResponse, cause: Throwable? = null) :
    GoogleGenerativeAIException(
        "Prompt was blocked: ${response.promptFeedback?.blockReason?.name}",
        cause,
    )

/**
 * The user's location (region) is not supported by the API.
 *
 * See the Google documentation for a
 * [list of regions](https://ai.google.dev/available_regions#available_regions) (countries and
 * territories) where the API is available.
 */
class UnsupportedUserLocationException(cause: Throwable? = null) :
    GoogleGenerativeAIException("User location is not supported for the API use.", cause)

/**
 * Some form of state occurred that shouldn't have.
 *
 * Usually indicative of consumer error.
 */
class InvalidStateException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/**
 * A request was stopped during generation for some reason.
 *
 * @property response the full server response for the request
 */
class ResponseStoppedException(val response: GenerateContentResponse, cause: Throwable? = null) :
    GoogleGenerativeAIException(
        "Content generation stopped. Reason: ${response.candidates?.first()?.finishReason?.name}",
        cause,
    )

/**
 * A request took too long to complete.
 *
 * Usually occurs due to a user specified [timeout][RequestOptions.timeout].
 */
class RequestTimeoutException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/** The quota for this API key is depleted, retry this request at a later time. */
class QuotaExceededException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/** The service is not enabled for this project. Visit the Firebase Console to enable it. */
class ServiceDisabledException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)

/** Catch all case for exceptions not explicitly expected. */
class UnknownException(message: String, cause: Throwable? = null) :
    GoogleGenerativeAIException(message, cause)
