package com.example.packetapp.utils

import com.example.packetapp.ui.viewmodel.ProcessedCheckItem

object ItemMatcher {
    fun matchItems(checkItems: List<ProcessedCheckItem>, groupItems: List<ProcessedCheckItem>): List<Pair<ProcessedCheckItem, ProcessedCheckItem?>> {
        val matchedItems = mutableListOf<Pair<ProcessedCheckItem, ProcessedCheckItem?>>()

        checkItems.forEach { checkItem ->
            val matchedGroupItem = groupItems.find { groupItem ->
                calculateSimilarity(checkItem.name, groupItem.name) >= 0.7
            }
            matchedItems.add(checkItem to matchedGroupItem)
        }

        return matchedItems
    }

    private fun calculateSimilarity(name1: String, name2: String): Double {
        val keywords1 = extractKeywords(name1)
        val keywords2 = extractKeywords(name2)

        val commonKeywords = keywords1.intersect(keywords2.toSet()).size.toDouble()
        val totalKeywords = (keywords1.size + keywords2.size - commonKeywords).toDouble()

        return if (totalKeywords == 0.0) 0.0 else commonKeywords / totalKeywords
    }

    private fun extractKeywords(name: String): List<String> {
        // Удаляем "шум" (например, "п/уп", "0,5кг" и т.д.)
        val noiseWords = listOf("п/уп", "нарез", "кг", "г", "л", "шт", "уп", "бокс", "сашет")
        val cleanedName = name.lowercase()
            .replace(Regex("[0-9,.]+(кг|г|л|шт|%)"), "")
            .replace(Regex("[\\(\\)\\[\\]:]"), "")
            .split(" ")
            .filter { it !in noiseWords && it.length > 2 }

        // Извлекаем ключевые слова (бренд, название, характеристика)
        return cleanedName
    }
}