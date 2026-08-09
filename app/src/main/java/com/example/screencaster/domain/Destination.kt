package com.example.screencaster.domain

object DestinationBuilder {
    fun publishUrl(serverUrl: String, streamKey: String): String {
        val server = serverUrl.trim().trimEnd('/')
        val key = streamKey.trim()
        require(server.isNotBlank()) { "Server URL is required." }
        require(key.isNotBlank()) { "Stream key is required." }
        StreamUrlValidator.validate(server).getOrThrow()
        return "$server/$key"
    }
}
