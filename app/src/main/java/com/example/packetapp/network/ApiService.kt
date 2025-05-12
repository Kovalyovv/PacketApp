package com.example.packetapp.network

import android.content.Context
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.models.Item
import com.example.packetapp.models.PersonalListItem
import com.example.packetapp.models.PurchaseHistoryItem
import com.example.packetapp.models.AddItemRequest
import com.example.packetapp.models.BuyItemRequest
import com.example.packetapp.models.ConfirmItemsRequest
import com.example.packetapp.models.ForgotPasswordRequest
import com.example.packetapp.models.JoinGroupRequest
import com.example.packetapp.models.UserLoginRequest
import com.example.packetapp.models.UserRegisterRequest
import com.example.packetapp.models.LoginResponse
import com.example.packetapp.models.UserDTO
import com.example.packetapp.ui.screens.GroupDTO
import com.example.packetapp.ui.viewmodel.ProcessedCheckData
import com.example.packetapp.ui.viewmodel.ProcessedCheckItem
import com.example.packetapp.ui.viewmodel.ReceiptDTO
import io.ktor.client.*

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiService(private val client: HttpClient, private val authManager: AuthManager) {
    private val baseUrl = "http://192.168.31.137:8080"

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

    suspend fun getUserGroups(accessToken: String, userId: Int): List<GroupDTO> {
        val response = client.get("$baseUrl/groups/user/$userId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body()
        } else {
            throw Exception("Ошибка получения групп: ${response.status.description}")
        }
    }

    fun getAccessTokenFromPrefs(context: Context): String {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("accessToken", "") ?: ""
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
    suspend fun getUserNameById(accessToken: String, userId: Int): String {
        return executeWithTokenRefresh { token ->
            val response = client.get("$baseUrl/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val user: UserDTO = response.body()
                    user.name
                }
                HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
                HttpStatusCode.NotFound -> "Пользователь $userId"
                else -> throw Exception("Ошибка получения имени пользователя: ${response.status.description}")
            }
        }
    }

    suspend fun getChatUserNames(accessToken: String, groupId: Int): Map<Int, String> {
        return executeWithTokenRefresh { token ->
            val response = client.get("$baseUrl/chats/$groupId/users") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val users: List<UserDTO> = response.body()
                    users.associate { it.id to it.name }
                }
                HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
                HttpStatusCode.NotFound -> emptyMap()
                else -> throw Exception("Ошибка получения пользователей чата: ${response.status.description}")
            }
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

    // Универсальная функция для выполнения запроса с обновлением токена
    private suspend fun <T> executeWithTokenRefresh(
        request: suspend (String) -> T
    ): T {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            return request(accessToken)
        } catch (e: Exception) {
            if (e.message == "Токен недействителен") {
                // Токен недействителен, пробуем обновить
                val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                val loginResponse = refreshToken(refreshToken)

                // Сохраняем новые токены
                authManager.saveTokens(loginResponse.token, loginResponse.refreshToken)

                // Повторяем запрос с новым токеном
                val newAccessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует после обновления")
                return request(newAccessToken)
            } else {
                throw e // Если это не ошибка 401, пробрасываем дальше
            }
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

    suspend fun getItemName(accessToken: String, itemId: Int): String {
        val response = client.get("$baseUrl/items/$itemId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body<String>()
        } else {
            throw Exception("Ошибка получения имени товара: ${response.status.description}")
        }
    }

    suspend fun addItemToPersonalList(accessToken: String, itemId: Int, itemName: String, quantity: Int, price: Int): PersonalListItem {
        val response = client.post("$baseUrl/personal-list") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(
                AddItemRequest(itemId, itemName, quantity, price)
            )
        }
        return when (response.status) {
            HttpStatusCode.Created -> response.body<PersonalListItem>()
            HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
            else -> throw Exception("Ошибка добавления в личный список: ${response.status.description}")
        }
    }

    suspend fun markAsPurchased(accessToken: String, itemId: Int) {
        val response = client.post("$baseUrl/personal-list/$itemId/mark-purchased") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
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

    suspend fun getGroupItemsReceipt(accessToken: String, groupId: Int): List<ProcessedCheckItem> {
        val response = client.get("$baseUrl/groups/$groupId/items-receipt") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<ProcessedCheckItem>>()
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

    @Serializable
    data class ScanReceiptResponse(
        val first: ReceiptDTO,
        val second: ProcessedCheckData,
        val message: String? = null
    )

    suspend fun getCheckDataFromServer(accessToken: String, qrCode: String): ScanReceiptResponse {
        return executeWithTokenRefresh { accessToken ->
            val response = client.post("$baseUrl/receipts/scan") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(Parameters.build {
                        append("qrCode", qrCode)
                        append("userId", authManager.getUserId().toString())
                    })
                )
            }

            println("Server response status: ${response.status}")
            println("Server response body: ${response.bodyAsText()}")
            val scanResponse = when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Conflict -> {
                    val result = response.body<ScanReceiptResponse>()
                    println("Deserialized checkData: ${result.second}")
                    result
                }

                HttpStatusCode.Unauthorized -> throw Exception("Токен недействителен")
                else -> throw Exception("Ошибка сервера: ${response.status.description}")
            }

            return@executeWithTokenRefresh scanResponse
        }

    }

    suspend fun confirmItems(accessToken: String, receiptId: Int, items: List<ProcessedCheckItem>, groupId: Int?) {
        println("Sending confirmItems request to $baseUrl/receipts/$receiptId/confirm: receiptId=$receiptId, groupId=$groupId, items=$items")
        val response = client.post("$baseUrl/receipts/$receiptId/confirm") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(ConfirmItemsRequest(receiptId, items, groupId))
        }
        println("Confirm items response status: ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Ошибка подтверждения товаров: ${response.status.description}")
        }
    }

    suspend fun addItemsToGroupActivity(accessToken: String, groupId: Int, items: List<ProcessedCheckItem>) {
        val response = client.post("$baseUrl/groups/$groupId/activity") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "items" to items
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Ошибка добавления товаров в активность группы: ${response.status.description}")
        }
    }

    suspend fun addItemsToPersonalList(accessToken: String, items: List<ProcessedCheckItem>) {
        items.forEach { item ->
            println("Sending item: $item")
            val response = client.post("$baseUrl/personal-list") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(
                    AddItemRequest(
                        itemId = null,
                        itemName = item.name,
                        quantity = item.quantity.toInt(),
                        price = item.price
                    )
                )
            }
            if (response.status != HttpStatusCode.OK) {
                throw Exception("Ошибка добавления товара ${item.name} в личный список: ${response.status.description}")
            }
        }
    }

    suspend fun getPersonalListItems(accessToken: String, userId: Int): List<ProcessedCheckItem> {
        val response = client.get("$baseUrl/personal-list/$userId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body()
        } else {
            throw Exception("Ошибка получения личного списка: ${response.status.description}")
        }
    }

    suspend fun getReceiptsHistory(accessToken: String, userId: Int): List<Pair<ReceiptDTO, ProcessedCheckData>> {
        val response = client.post("$baseUrl/receipts/history") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        if (!response.status.isSuccess()) {
            throw Exception("Ошибка загрузки истории: ${response.status.description} (Status: ${response.status.value})")
        }

        return response.body() // Прямое десериализация в List<Pair<ReceiptDTO, ProcessedCheckData>>
    }
}

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class ReceiptResponse(
    val receipt: ReceiptDTO,
    val checkData: ProcessedCheckData
)

@Serializable
data class JoinGroupResponse(
    val groupId: Int,
    val groupName: String,
    val message: String
)