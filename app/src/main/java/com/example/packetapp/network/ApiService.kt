package com.example.packetapp.network



import android.content.Context
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.models.ForgotPasswordRequest
import com.example.packetapp.network.models.UserLoginRequest
import com.example.packetapp.network.models.UserRegisterRequest
import com.example.packetapp.network.models.LoginResponse
import com.example.packetapp.network.models.UserDTO
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


class ApiService(private val client: HttpClient) {
    private val baseUrl = "http://10.0.2.2:8080" // Замените на ваш URL сервера
//    private val baseUrl = "http://127.0.0.1:8080" // Замените на ваш URL сервера


    suspend fun login(email: String, password: String): LoginResponse {
        val response = client.post("$baseUrl/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(email, password))
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<LoginResponse>()
            HttpStatusCode.Unauthorized -> {
                val errorBody = response.bodyAsText()
                // Парсим JSON вручную, чтобы извлечь поле message
                val jsonElement = Json.parseToJsonElement(errorBody)
                val errorMessage = jsonElement.jsonObject["message"]?.jsonPrimitive?.content
                    ?: "Неизвестная ошибка"
                throw Exception(errorMessage)
            }
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }


    suspend fun register(name: String, email: String, password: String, role: String = "standard"): LoginResponse {
        val response = client.post("$baseUrl/users/register") {
            contentType(ContentType.Application.Json)
            setBody(UserRegisterRequest(name, email, password, role))
        }
        return when (response.status) {
            HttpStatusCode.Created -> response.body<LoginResponse>()
            HttpStatusCode.Conflict -> throw Exception("Пользователь с таким email уже существует")
            HttpStatusCode.BadRequest -> {
                val errorBody = response.bodyAsText()
                throw Exception(Json.decodeFromString<ErrorResponse>(errorBody).error)
            }
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun requestResetCode(email: String): String {
        val response = client.post("$baseUrl/users/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val successBody = response.bodyAsText()
                val jsonElement = Json.parseToJsonElement(successBody)
                jsonElement.jsonObject["message"]?.jsonPrimitive?.content
                    ?: "Код отправлен"
            }
            HttpStatusCode.NotFound -> throw Exception("Пользователь с таким email не найден")
            HttpStatusCode.BadRequest -> {
                val errorBody = response.bodyAsText()
                val jsonElement = Json.parseToJsonElement(errorBody)
                val errorMessage = jsonElement.jsonObject["error"]?.jsonPrimitive?.content
                    ?: "Ошибка запроса"
                throw Exception(errorMessage)
            }
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getUserProfile(accessToken: String): UserDTO {
        val response = client.get("$baseUrl/users/profile") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<UserDTO>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun resetPassword(code: String, newPassword: String) {
        val response = client.post("$baseUrl/users/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("code" to code, "newPassword" to newPassword))
        }
        when (response.status) {
            HttpStatusCode.OK -> Unit
            HttpStatusCode.BadRequest -> throw Exception("Неверный код восстановления")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }


    suspend fun refreshToken(refreshToken: String): LoginResponse {
        val response = client.post("$baseUrl/users/refresh-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<LoginResponse>()
            HttpStatusCode.Unauthorized -> throw Exception("Недействительный refresh-токен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }
    
}

@Serializable
data class ErrorResponse(val error: String)