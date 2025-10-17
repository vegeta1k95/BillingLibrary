package com.sdk.billinglibrary

import android.content.Context
import java.util.regex.Pattern

enum class Interval { DAY, WEEK, MONTH, YEAR }

class Period {

    var days = 0
    var weeks = 0
    var months = 0
    var years = 0

    fun totalDays(): Double {
        return (years * DAYS_PER_YEAR) +
               (months * DAYS_PER_MONTH) +
               (weeks * DAYS_PER_WEEK) +
               (days.toDouble())
    }

    fun toReadableString(context: Context?) : String {
        if (context == null) return ""
        return when {
            years  == 1 -> context.resources.getString(R.string.year)
            years   > 0 -> context.resources.getQuantityString(R.plurals.years, years, years)
            months == 1 -> context.resources.getString(R.string.month)
            months  > 0 -> context.resources.getQuantityString(R.plurals.months, months, months)
            weeks  == 1 -> context.resources.getString(R.string.week)
            weeks   > 0 -> context.resources.getQuantityString(R.plurals.weeks, weeks, weeks)
            days   == 1 -> context.resources.getString(R.string.day)
            days    > 0 -> context.resources.getQuantityString(R.plurals.days, days, days)
            else -> ""
        }
    }

    companion object {

        internal const val DAYS_PER_WEEK = 7.0
        internal const val DAYS_PER_YEAR = 365.25
        internal const val DAYS_PER_MONTH = DAYS_PER_YEAR / 12.0

        private val PATTERN = Pattern.compile(
            "P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?",
            2
        )

        private fun parseNumber(str: String?): Int {
            return str?.toInt() ?: 0
        }

        fun parse(text: CharSequence): Period {
            val period = Period()
            val matcher = PATTERN.matcher(text)
            if (matcher.matches()) {
                val yearMatch = matcher.group(1)
                val monthMatch = matcher.group(2)
                val weekMatch = matcher.group(3)
                val dayMatch = matcher.group(4)
                if (yearMatch != null || monthMatch != null || weekMatch != null || dayMatch != null) {
                    period.years = yearMatch?.toInt() ?: 0
                    period.months = monthMatch?.toInt() ?: 0
                    period.weeks = weekMatch?.toInt() ?: 0
                    period.days = dayMatch?.toInt() ?: 0
                }
            }
            return period
        }
    }
}
