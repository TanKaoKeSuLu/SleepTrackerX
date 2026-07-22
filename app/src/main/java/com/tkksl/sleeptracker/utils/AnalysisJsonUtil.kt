package com.tkksl.sleeptracker.utils

import com.google.gson.Gson
import com.tkksl.sleeptracker.data.analyzer.SleepAnalysis

object AnalysisJsonUtil {
    private val gson = Gson()

    fun toJson(analysis: SleepAnalysis): String {
        return gson.toJson(analysis)
    }

    fun fromJson(json: String?): SleepAnalysis? {
        if (json.isNullOrBlank()) return null
        return gson.fromJson(json, SleepAnalysis::class.java)
    }
}