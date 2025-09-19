package com.example.pointage.ui.historique;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static Date parseSupabaseTimestamp(String timestampStr) throws ParseException {
        if (timestampStr == null) throw new ParseException("Null timestamp", 0);
        String ts = timestampStr.trim();

        // Try ISO 8601 without timezone: yyyy-MM-dd'T'HH:mm:ss
        try {
            SimpleDateFormat isoNoTz = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            isoNoTz.setLenient(false);
            return isoNoTz.parse(ts.length() >= 19 ? ts.substring(0, 19) : ts);
        } catch (ParseException ignored) {}

        // Try ISO 8601 with 'Z' or timezone: yyyy-MM-dd'T'HH:mm:ssX
        try {
            SimpleDateFormat isoTz = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
            isoTz.setLenient(false);
            return isoTz.parse(ts);
        } catch (ParseException ignored) {}

        // Fallback: space separated
        try {
            SimpleDateFormat space = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            space.setLenient(false);
            return space.parse(ts.replace('T', ' ').length() >= 19 ? ts.replace('T', ' ').substring(0, 19) : ts.replace('T', ' '));
        } catch (ParseException e) {
            throw e;
        }
    }

    public static String formatForSupabase(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return isoFormat.format(date);
    }

    public static Date parseTime(String timeStr) throws ParseException {
        if (timeStr == null) throw new ParseException("Null time", 0);
        String s = timeStr.trim();

        // If string looks like a full timestamp (contains date or 'T'), parse as ISO timestamp
        if (s.contains("T") || s.contains("-")) {
            return parseSupabaseTimestamp(s);
        }

        // Accept both HH:mm:ss and HH:mm
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public static String getSession() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        return (hourOfDay < 12) ? "Matin" : "Après-midi";
    }
}