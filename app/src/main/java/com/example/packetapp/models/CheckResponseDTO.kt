package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class CheckResponseDTO(
    val code: Int,
    val first: Int? = null,
    val data: CheckDataDTO? = null,
    val request: CheckRequestDTO? = null
)

@Serializable
data class CheckDataDTO(
    val json: CheckJsonDTO? = null,
    val html: String? = null
)

@Serializable
data class CheckJsonDTO(
    val totalSum: Double? = null, // Общая сумма чека
    val user: String? = null, // Название организации
    val dateTime: String? = null, // Дата и время покупки
    val retailPlace: String? = null, // Название магазина
    val items: List<CheckItemDTO>? = null // Список товаров
)

@Serializable
data class CheckItemDTO(
    val name: String? = null, // Название товара
    val price: Double? = null, // Цена за единицу
    val quantity: Double? = null, // Количество
    val sum: Double? = null, // Общая сумма за товар (не используем, но оставим для полноты)
    val nds: Int? = null, // НДС (не используем)
    val paymentType: Int? = null, // Тип оплаты (не используем)
    val productType: Int? = null, // Тип продукта (не используем)
    val productCodeNew: ProductCodeNewDTO? = null, // Код продукта (не используем)
    val itemsQuantityMeasure: Int? = null // Единица измерения (не используем)
)

@Serializable
data class ProductCodeNewDTO(
    val ean13: Ean13DTO? = null,
    val gs1m: Gs1mDTO? = null
)

@Serializable
data class Ean13DTO(
    val gtin: String? = null,
    val sernum: String? = null,
    val productIdType: Int? = null,
    val rawProductCode: String? = null
)

@Serializable
data class Gs1mDTO(
    val gtin: String? = null,
    val sernum: String? = null,
    val productIdType: Int? = null,
    val rawProductCode: String? = null
)

@Serializable
data class CheckRequestDTO(
    val qrurl: String? = null,
    val qrfile: String? = null,
    val qrraw: String? = null,
    val manual: CheckManualDTO? = null
)

@Serializable
data class CheckManualDTO(
    val fn: String? = null,
    val fd: String? = null,
    val fp: String? = null,
    val check_time: String? = null,
    val type: String? = null,
    val sum: String? = null
)