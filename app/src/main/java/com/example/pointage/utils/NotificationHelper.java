package com.example.pointage.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.util.Log;
import com.example.pointage.R;
import com.example.pointage.MainActivity;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;

public class NotificationHelper {
    private static final String CHANNEL_ID = "pointage_notifications";
    private static final String CHANNEL_NAME = "Notifications Pointage";
    private static final int NOTIFICATION_ID_PRESENCE = 1001;
    private static final int NOTIFICATION_ID_RETARD = 1002;
    private static final int NOTIFICATION_ID_ABSENCE = 1003;
    
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_DISPLAYED_NOTIFICATIONS = "displayed_notifications";
    
    private static NotificationManager notificationManager;
    private static Context appContext; // Contexte global pour appels ultérieurs
    private static SharedPreferences sharedPreferences;
    private static final Set<String> displayedNotifications = new HashSet<>();

    /**
     * Initialiser le NotificationHelper (appeler une fois au démarrage)
     */
    public static void initialize(Context context) {
        appContext = context.getApplicationContext(); // Stocker le contexte de l'app
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        createNotificationChannel(context);
        loadDisplayedNotifications();
    }

    /**
     * Charger les notifications déjà affichées depuis SharedPreferences
     */
    private static void loadDisplayedNotifications() {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(KEY_DISPLAYED_NOTIFICATIONS, "");
            if (!json.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    String[] notified = gson.fromJson(json, String[].class);
                    if (notified != null) {
                        for (String key : notified) {
                            displayedNotifications.add(key);
                        }
                        Log.d("NotificationHelper", "Loaded " + displayedNotifications.size() + " previously displayed notifications");
                    }
                } catch (Exception e) {
                    Log.e("NotificationHelper", "Error loading displayed notifications", e);
                }
            }
        }
    }

    /**
     * Sauvegarder les notifications affichées dans SharedPreferences
     */
    private static void saveDisplayedNotifications() {
        if (sharedPreferences != null) {
            try {
                Gson gson = new Gson();
                String json = gson.toJson(displayedNotifications.toArray());
                sharedPreferences.edit().putString(KEY_DISPLAYED_NOTIFICATIONS, json).apply();
                Log.d("NotificationHelper", "Saved " + displayedNotifications.size() + " displayed notifications");
            } catch (Exception e) {
                Log.e("NotificationHelper", "Error saving displayed notifications", e);
            }
        }
    }

    /**
     * Vérifier si une notification a déjà été affichée
     */
    private static boolean isAlreadyDisplayed(String notificationKey) {
        return displayedNotifications.contains(notificationKey);
    }

    /**
     * Marquer une notification comme affichée
     */
    private static void markAsDisplayed(String notificationKey) {
        if (!displayedNotifications.contains(notificationKey)) {
            displayedNotifications.add(notificationKey);
            saveDisplayedNotifications();
        }
    }

    /**
     * Créer un canal de notification pour Android 8+
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications des pointages et absences");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Vérifier si la permission POST_NOTIFICATIONS est accordée (Android 13+)
     */
    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33+)
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Sur Android 12 et avant, la permission n'est pas nécessaire
        return true;
    }

    /**
     * Afficher une notification externe (système)
     *
     * @param context Contexte Android
     * @param title Titre de la notification
     * @param message Message de la notification
     * @param type Type de notification ("presence", "retard", "absence")
     */
    public static void showNotification(Context context, String title, String message, String type) {
        if (notificationManager == null) {
            initialize(context);
        }

        // Vérifier la permission avant d'afficher
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "Permission POST_NOTIFICATIONS non accordée");
            return;
        }

        // Déterminer l'icône, la couleur et le son selon le type
        int smallIcon = R.drawable.ic_launcher_foreground; // Icône par défaut
        int color = 0xFF1E40AF; // Couleur primaire par défaut
        int notificationId = NOTIFICATION_ID_PRESENCE; // Par défaut
        
        if ("retard".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_retard; // Icône avec horloge orange
            color = 0xFFFF9800; // Orange
            notificationId = NOTIFICATION_ID_RETARD;
        } else if ("absence".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_absence; // Icône X rouge
            color = 0xFFFF5252; // Rouge
            notificationId = NOTIFICATION_ID_ABSENCE;
        } else if ("presence".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_presence; // Icône checkmark vert
            color = 0xFF4CAF50; // Vert
            notificationId = NOTIFICATION_ID_PRESENCE;
        }

        // Créer un intent pour ouvrir MainActivity au clic sur la notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Créer le PendingIntent avec FLAG_IMMUTABLE pour sécurité
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(color)
                .setVibrate(new long[]{0, 500, 250, 500}) // Vibration pattern
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent); // Action au clic : ouvrir MainActivity

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }


    public static void showNotification(Context context, String title, String message, String type, int notificationId) {
        if (notificationManager == null) {
            initialize(context);
        }

        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "Permission POST_NOTIFICATIONS non accordée");
            return;
        }

        int smallIcon = R.drawable.ic_launcher_foreground; // Icône par défaut
        int color = 0xFF1E40AF; // Couleur primaire par défaut

        if ("retard".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_retard; // Icône avec horloge orange
            color = 0xFFFF9800; // Orange
        } else if ("absence".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_absence; // Icône X rouge
            color = 0xFFFF5252; // Rouge
        } else if ("présence".equalsIgnoreCase(type)) {
            smallIcon = R.drawable.ic_presence; // Icône checkmark vert
            color = 0xFF4CAF50; // Vert
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(color)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    /**
     * Afficher une notification d'absence (sans contexte - utilise le contexte global)
     * @param surveillantId ID du surveillant (pour générer une clé unique)
     * @param nomSurveillant Nom du surveillant
     * @param numeroSalle Numéro de la salle
     * @param session Session (Matin/Après-midi)
     * @param date Date de l'absence au format "yyyy-MM-dd"
     */
    public static void showAbsenceNotification(Long surveillantId, String nomSurveillant, String numeroSalle, String session, String date) {
        if (appContext != null) {
            showAbsenceNotification(appContext, surveillantId, nomSurveillant, numeroSalle, session, date);
        }
    }

    /**
     * Afficher une notification d'absence (avec contexte explicite)
     */
    public static void showAbsenceNotification(Context context, Long surveillantId, String nomSurveillant, String numeroSalle, String session, String date) {
        // Créer une clé unique pour cette absence
        String notificationKey = surveillantId + "_absence_" + date + "_" + (session != null ? session : "");
        
        // Vérifier si cette notification a déjà été affichée
        if (isAlreadyDisplayed(notificationKey)) {
            Log.d("NotificationHelper", "Absence notification already displayed: " + notificationKey);
            return;
        }
        
        String title = "❌ Absence détectée";
        String message = nomSurveillant + " - Salle " + numeroSalle + " (" + (session != null ? session : "N/A") + ")";
        int uniqueId = Math.abs(notificationKey.hashCode());
        showNotification(context, title, message, "absence", uniqueId);
        
        // Marquer comme affichée
        markAsDisplayed(notificationKey);
    }

    /**
     * Afficher une notification de retard (sans contexte - utilise le contexte global)
     * @param surveillantId ID du surveillant (pour générer une clé unique)
     * @param nomSurveillant Nom du surveillant
     * @param numeroSalle Numéro de la salle
     * @param date Date du retard au format "yyyy-MM-dd"
     */
    public static void showRetardNotification(Long surveillantId, String nomSurveillant, String numeroSalle, String date) {
        if (appContext != null) {
            showRetardNotification(appContext, surveillantId, nomSurveillant, numeroSalle, date);
        }
    }

    /**
     * Afficher une notification de retard (avec contexte explicite)
     */
    public static void showRetardNotification(Context context, Long surveillantId, String nomSurveillant, String numeroSalle, String date) {
        // Créer une clé unique pour ce retard
        String notificationKey = surveillantId + "_retard_" + date;
        
        // Vérifier si cette notification a déjà été affichée
        if (isAlreadyDisplayed(notificationKey)) {
            Log.d("NotificationHelper", "Retard notification already displayed: " + notificationKey);
            return;
        }
        
        String title = "⏰ Retard détecté";
        String message = nomSurveillant + " - Salle " + numeroSalle;
        int uniqueId = Math.abs(notificationKey.hashCode());
        showNotification(context, title, message, "retard", uniqueId);
        
        // Marquer comme affichée
        markAsDisplayed(notificationKey);
    }

    /**
     * Afficher une notification de présence (sans contexte - utilise le contexte global)
     * @param surveillantId ID du surveillant (pour générer une clé unique)
     * @param nomSurveillant Nom du surveillant
     * @param numeroSalle Numéro de la salle
     * @param date Date de la présence au format "yyyy-MM-dd"
     */
    public static void showPresenceNotification(Long surveillantId, String nomSurveillant, String numeroSalle, String date) {
        if (appContext != null) {
            showPresenceNotification(appContext, surveillantId, nomSurveillant, numeroSalle, date);
        }
    }

    /**
     * Afficher une notification de présence (avec contexte explicite)
     */
    public static void showPresenceNotification(Context context, Long surveillantId, String nomSurveillant, String numeroSalle, String date) {
        // Créer une clé unique pour cette présence
        String notificationKey = surveillantId + "_presence_" + date;
        
        // Vérifier si cette notification a déjà été affichée
        if (isAlreadyDisplayed(notificationKey)) {
            Log.d("NotificationHelper", "Presence notification already displayed: " + notificationKey);
            return;
        }
        
        String title = "✅ Présence enregistrée";
        String message = nomSurveillant + " - Salle " + numeroSalle;
        int uniqueId = Math.abs(notificationKey.hashCode());
        showNotification(context, title, message, "presence", uniqueId);
        
        // Marquer comme affichée
        markAsDisplayed(notificationKey);
    }

    /**
     * Retourner le contexte global de l'application (stocké lors de l'initialisation)
     */
    public static Context getAppContext() {
        return appContext;
    }
}