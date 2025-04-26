package com.example.packetapp.network

import android.content.Context
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.models.Item
import com.example.packetapp.models.PersonalListItem
import com.example.packetapp.models.PurchaseHistoryItem
import com.example.packetapp.network.models.BuyItemRequest
import com.example.packetapp.network.models.ForgotPasswordRequest
import com.example.packetapp.network.models.JoinGroupRequest
import com.example.packetapp.network.models.UserLoginRequest
import com.example.packetapp.network.models.UserRegisterRequest
import com.example.packetapp.network.models.LoginResponse
import com.example.packetapp.network.models.UserDTO
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiService(private val client: HttpClient, private val authManager: AuthManager) {
    private val baseUrl = "http://10.0.2.2:8080" // Замени на актуальный URL сервера

    suspend fun login(email: String, password: String): LoginResponse {
        val response = client.post("$baseUrl/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(email, password))
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<LoginResponse>()
            HttpStatusCode.Unauthorized -> {
                val errorBody = response.bodyAsText()
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

    suspend fun joinGroup(accessToken: String, request: JoinGroupRequest): JoinGroupResponse {
        val response = client.post("$baseUrl/groups/join") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<JoinGroupResponse>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            HttpStatusCode.BadRequest -> {
                val errorBody = response.bodyAsText()
                val jsonElement = Json.parseToJsonElement(errorBody)
                val errorMessage = jsonElement.jsonObject["error"]?.jsonPrimitive?.content
                    ?: "Неверный инвайт-код"
                throw Exception(errorMessage)
            }
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun searchItems(accessToken: String, query: String): List<Item> {
        val response = client.get("$baseUrl/items/search") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("query", query)
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<Item>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getPersonalList(accessToken: String): List<PersonalListItem> {
        val response = client.get("$baseUrl/personal-list") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<PersonalListItem>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun addItemToPersonalList(accessToken: String, itemId: Int, quantity: Int): PersonalListItem {
        val response = client.post("$baseUrl/personal-list") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "itemId" to itemId,
                "quantity" to quantity
            ))
        }
        return when (response.status) {
            HttpStatusCode.Created -> response.body<PersonalListItem>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun markAsPurchased(accessToken: String, itemId: Int, price: Int) {
        val response = client.post("$baseUrl/personal-list/$itemId/mark-purchased") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf("price" to price))
        }
        when (response.status) {
            HttpStatusCode.OK -> return
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            HttpStatusCode.NotFound -> throw Exception("Элемент не найден")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getPurchaseHistory(accessToken: String): List<PurchaseHistoryItem> {
        val response = client.get("$baseUrl/personal-purchase-history") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<PurchaseHistoryItem>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun markItemsAsViewed(accessToken: String, groupId: Int, itemIds: List<Int>) {
        val response = client.post("$baseUrl/groups/$groupId/mark-viewed") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf("itemIds" to itemIds))
        }
        when (response.status) {
            HttpStatusCode.OK -> return
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun markAllActivitiesAsViewed(accessToken: String, groupId: Int) {
        val response = client.post("$baseUrl/groups/$groupId/mark-all-viewed") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        when (response.status) {
            HttpStatusCode.OK -> return
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getGroupItems(accessToken: String, groupId: Int): List<GroupListItem> {
        val response = client.get("$baseUrl/groups/$groupId/items") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<GroupListItem>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getGroupSummaries(accessToken: String): List<GroupSummary> {
        val response = client.get("$baseUrl/groups/summaries") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<GroupSummary>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun addItemToGroup(accessToken: String, groupId: Int, itemId: Int, quantity: Int, priority: Int, budget: Int?) {
        val response = client.post("$baseUrl/groups/$groupId/items") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "itemId" to itemId,
                "quantity" to quantity,
                "priority" to priority,
                "budget" to budget
            ))
        }
        when (response.status) {
            HttpStatusCode.Created -> return
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun buyItem(accessToken: String, groupId: Int, itemId: Int, request: BuyItemRequest) {
        val response = client.post("$baseUrl/groups/$groupId/items/$itemId/buy") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        when (response.status) {
            HttpStatusCode.OK -> return
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }

    suspend fun getUserGroups(accessToken: String): List<GroupSummary> {
        val response = client.get("$baseUrl/groups/summaries") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<GroupSummary>>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка сервера: ${response.status.description}")
        }
    }
}

@Serializable
data class ErrorResponse(val error: String)



@Serializable
data class JoinGroupResponse(
    val groupId: Int,
    val groupName: String,
    val message: String
)