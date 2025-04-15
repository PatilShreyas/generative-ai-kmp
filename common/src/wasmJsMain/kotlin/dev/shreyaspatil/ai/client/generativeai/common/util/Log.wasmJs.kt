package dev.shreyaspatil.ai.client.generativeai.common.util

actual object Log {
    actual fun w(tag: String, message: String) {
        println("[$tag] $message")
    }
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] $message")
        throwable?.printStackTrace()
    }
}