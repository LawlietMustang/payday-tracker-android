package com.paydaytracker.app.data.db

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        if (list == null) return "[]"
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<Int>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getInt(i))
            }
        } catch (_: Exception) {
            value.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { list.add(it) }
        }
        return list
    }

    @TypeConverter
    fun fromMapStringDouble(map: Map<String, Double>?): String {
        if (map == null) return "{}"
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun toMapStringDouble(value: String?): Map<String, Double> {
        if (value.isNullOrEmpty()) return emptyMap()
        val map = mutableMapOf<String, Double>()
        try {
            val obj = JSONObject(value)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getDouble(key)
            }
        } catch (_: Exception) {}
        return map
    }
}
