package com.example.packetapp.network

import com.example.packetapp.data.AuthManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json

object KtorClient {
    private var authManager: AuthManager? = null
    private var isInitialized = false

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingInterval = 15_000
            maxFrameSize = Long.MAX_VALUE

        }
    }

    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
        this.isInitialized = true
    }

    val apiService: ApiService by lazy {
        check(isInitialized) { "KtorClient must be initialized with AuthManager before accessing apiService" }
        ApiService(httpClient, authManager!!)
    }
}





//package com.example.packetapp.network
//
//import com.example.packetapp.data.AuthManager
//import io.ktor.client.*
//import io.ktor.client.engine.android.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.client.plugins.logging.*
//import io.ktor.client.plugins.websocket.*
//import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
//import io.ktor.serialization.kotlinx.json.*
//import kotlinx.serialization.json.Json
//
//object KtorClient {
//    private var authManager: AuthManager? = null
//    private var isInitialized = false
//
//    val httpClient = HttpClient(Android) {
//        install(ContentNegotiation) {
//            json(Json {
//                prettyPrint = true
//                isLenient = true
//                ignoreUnknownKeys = true
//            })
//        }
//        install(Logging) {
//            logger = Logger.DEFAULT
//            level = LogLevel.ALL
//        }
//        install(WebSockets) {
//            contentConverter = KotlinxWebsocketSerializationConverter(Json)
//            pingInterval = 15_000 // 15 секунд в миллисекундах
//            pingInterval = 15_000L // Альтернативный способ
//            maxFrameSize = Long.MAX_VALUE
//
//        }
//    }
//
//    fun initialize(authManager: AuthManager) {
//        this.authManager = authManager
//        this.isInitialized = true
//    }
//
//    val apiService: ApiService by lazy {
//        check(isInitialized) { "KtorClient must be initialized with AuthManager before accessing apiService" }
//        ApiService(httpClient, authManager!!)
//    }
//}