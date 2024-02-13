package com.sdk.billinglibrary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Period {

    int days = 0;
    int weeks = 0;
    int months = 0;
    int years = 0;

    private static final Pattern PATTERN = Pattern.compile(
            "P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?",
            2);

    private static int parseNumber(String str) {
        return str == null ? 0 : Integer.parseInt(str);
    }

    public static Period parse(CharSequence text) {

        Period period = new Period();

        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            String yearMatch = matcher.group(1);
            String monthMatch = matcher.group(2);
            String weekMatch = matcher.group(3);
            String dayMatch = matcher.group(4);
            if (yearMatch != null || monthMatch != null || weekMatch != null || dayMatch != null) {
                period.years = parseNumber(yearMatch);
                period.months = parseNumber(monthMatch);
                period.weeks = parseNumber(weekMatch);
                period.days = parseNumber(dayMatch);
            }
        }

        return period;
    }

}
