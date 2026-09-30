package com.example.pointage.ui.historique;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    public static Date parseSupabaseTimestamp(String timestampStr) throws ParseException {
        if (timestampStr == null) throw new ParseException("Null timestamp", 0);
        String ts = timestampStr.trim();


        if (ts.endsWith("Z") || ts.endsWith("z") || ts.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            ParseException last = null;
            for (String pattern : new String[]{
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX"
            }) {
                try {
                    SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.getDefault());
                    fmt.setLenient(false);
                    return fmt.parse(ts);
                } catch (ParseException e) { last = e; }
            }
            if (last != null) throw last;
        }


        ParseException lastNoTz = null;
        for (String pattern : new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss"
        }) {
            try {
                SimpleDateFormat isoNoTz = new SimpleDateFormat(pattern, Locale.getDefault());
                isoNoTz.setLenient(false);

                int maxLen = pattern.length();
                String candidate = ts;
                if (candidate.length() > 19 && pattern.endsWith(".SSS")) {
                    // keep milliseconds if present
                } else if (candidate.length() > 19) {
                    candidate = candidate.substring(0, 19);
                }
                return isoNoTz.parse(candidate);
            } catch (ParseException e) { lastNoTz = e; }
        }


        try {
            String spaceStr = ts.replace('T', ' ');
            ParseException last = null;
            for (String pattern : new String[]{
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd HH:mm:ss"
            }) {
                try {
                    SimpleDateFormat space = new SimpleDateFormat(pattern, Locale.getDefault());
                    space.setLenient(false);
                    String candidate = spaceStr;
                    if (candidate.length() > 19 && pattern.endsWith(".SSS")) {
                        // ok
                    } else if (candidate.length() > 19) {
                        candidate = candidate.substring(0, 19);
                    }
                    return space.parse(candidate);
                } catch (ParseException e) { last = e; }
            }
            throw last != null ? last : new ParseException("Unparseable timestamp", 0);
        } catch (ParseException e) {
            throw e;
        }
    }

    public static String formatForSupabase(Date date) {
        // Conserver un timestamp sans fuseau pour correspondre aux colonnes timestamp (sans tz)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        isoFormat.setLenient(false);
        return isoFormat.format(date);
    }

    public static Date parseTime(String timeStr) throws ParseException {
        if (timeStr == null) throw new ParseException("Null time", 0);
        String s = timeStr.trim();


        if (s.contains("T") || s.contains("-")) {
            return parseSupabaseTimestamp(s);
        }

        ParseException last = null;
        for (String pattern : new String[]{"HH:mm:ss", "HH:mm"}) {
            try {
                SimpleDateFormat timeFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                timeFormat.setLenient(false);
                return timeFormat.parse(s);
            } catch (ParseException e) { last = e; }
        }
        throw last != null ? last : new ParseException("Unparseable time: " + s, 0);
    }

    public static String getCurrentDateString() {
        return formatDateOnly(new Date());
    }

    public static String formatDateOnly(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));
        return dateFormat.format(date);
    }

    public static String getSession() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        return (hourOfDay < 12) ? "Matin" : "Après-midi";
    }
}