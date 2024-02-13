package com.sdk.billinglibrary

import java.util.regex.Pattern

class Period {

    var days = 0
    var weeks = 0
    var months = 0
    var years = 0

    companion object {
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
                    period.years = parseNumber(yearMatch)
                    period.months = parseNumber(monthMatch)
                    period.weeks = parseNumber(weekMatch)
                    period.days = parseNumber(dayMatch)
                }
            }
            return period
        }
    }
}
