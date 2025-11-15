package com.example.pointage.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.SupabaseClient;
import com.example.pointage.ui.historique.DateUtils;
import com.example.pointage.ui.historique.HistoriqueViewModel;
import com.example.pointage.utils.NotificationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";
    private static final String PREFS_NAME = "home_notifications_prefs";
    private static final String NOTIFIED_NOTIFICATIONS_KEY = "notified_notifications";
    
    private final MutableLiveData<String> mText;
    private final MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();
    private SupabaseClient supabaseClient;
    private Context context;
    private List<Notification> previousNotifications = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Set<String> notifiedNotifications = new HashSet<>();
    
    // Auto-refresh
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final long refreshIntervalMs = 10_000; // 30 secondes
    private volatile boolean isLoading = false;
    
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadNotifications();
            checkPassedExamAbsences(); // Vérifier les absences pour les examens déjà passés
            refreshHandler.postDelayed(this, refreshIntervalMs);
        }
    };

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("POINTAGES DES SURVEILLANTS DE L'ENI");

        try {
            supabaseClient = SupabaseClient.getInstance();
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Failed to initialize SupabaseClient", e);
            supabaseClient = null;
        }

        if (supabaseClient != null) {
            // Charger les notifications initiales
            loadNotifications();
            // Vérifier les absences pour examens déjà passés au démarrage
            checkPassedExamAbsences();
        }

        // Démarrer l'auto-refresh
        refreshHandler.postDelayed(refreshRunnable, refreshIntervalMs);
    }

    public LiveData<String> getText() {
        return mText;
    }
    
    public LiveData<List<Notification>> getNotifications() {
        return notificationsLiveData;
    }
    
    /**
     * Définir le contexte pour envoyer les notifications externes
     */
    public void setContext(Context context) {
        this.context = context;
        if (sharedPreferences == null) {
            initializeSharedPreferences();
        }
    }

    /**
     * Initialiser SharedPreferences et charger les notifications déjà vues
     */
    public void initializeSharedPreferences() {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot initialize SharedPreferences");
            return;
        }
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            loadNotifiedNotificationsFromPrefs();
        }
    }

    /**
     * Charger les notifications déjà vues depuis SharedPreferences
     */
    private void loadNotifiedNotificationsFromPrefs() {
        if (sharedPreferences != null) {
            notifiedNotifications = new HashSet<>(sharedPreferences.getStringSet(NOTIFIED_NOTIFICATIONS_KEY, new HashSet<>()));
            Log.d(TAG, "Loaded " + notifiedNotifications.size() + " previously notified notifications from prefs");
        }
    }

    public void loadNotifications() {
        if (isLoading) {
            Log.d(TAG, "Skip refresh: still loading");
            return;
        }
        isLoading = true;
        
        final List<Notification> notifications = new ArrayList<>();
        final Map<Long, String> surveillantNameById = new HashMap<>();
        final Map<Long, String> surveillantRoomById = new HashMap<>();

        // Calculer la date limite (60 jours en arrière)
        Calendar limitCal = Calendar.getInstance();
        limitCal.add(Calendar.DAY_OF_MONTH, -60);
        final Date limitDate = limitCal.getTime();

        // 1️⃣ Récupérer tous les surveillants
        supabaseClient.select("surveillant", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray surveillantResult) {
                try {
                    for (int i = 0; i < surveillantResult.size(); i++) {
                        JsonObject surveillantDoc = surveillantResult.get(i).getAsJsonObject();
                        Long surveillantId = surveillantDoc.get("id_surveillant").getAsLong();
                        String nomSurveillant = surveillantDoc.get("nom_surveillant").getAsString();

                        surveillantNameById.put(surveillantId, nomSurveillant);
                    }

                    // 2️⃣ Récupérer les salles depuis planning_surveillance
                    loadSurveillantRoomsFromPlanning(notifications, surveillantNameById, surveillantRoomById, limitDate);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing surveillants", e);
                    isLoading = false;
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error loading surveillants", error);
                isLoading = false;
            }
        });
    }

    private void loadSurveillantRoomsFromPlanning(List<Notification> notifications,
                                                  Map<Long, String> surveillantNameById,
                                                  Map<Long, String> surveillantRoomById,
                                                  Date limitDate) {
        // Récupérer les affectations surveillant/salle depuis planning_surveillance
        supabaseClient.select("planning_surveillance", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                try {
                    for (int i = 0; i < planningResult.size(); i++) {
                        JsonObject planningDoc = planningResult.get(i).getAsJsonObject();
                        if (!planningDoc.has("id_surveillant") || planningDoc.get("id_surveillant").isJsonNull()) {
                            continue;
                        }
                        Long surveillantId = planningDoc.get("id_surveillant").getAsLong();

                        if (!surveillantNameById.containsKey(surveillantId)) {
                            continue; // Surveillant inconnu
                        }

                        if (planningDoc.has("numero_salle") && !planningDoc.get("numero_salle").isJsonNull()) {
                            String numeroSalle = planningDoc.get("numero_salle").getAsString();
                            if (numeroSalle != null && !numeroSalle.trim().isEmpty()) {
                                surveillantRoomById.put(surveillantId, numeroSalle.trim());
                            }
                        }
                    }

                    loadPointageNotifications(notifications, surveillantNameById, surveillantRoomById, limitDate);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing planning_surveillance", e);
                    isLoading = false;
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error loading planning_surveillance", error);
                isLoading = false;
            }
        });
    }

    private void loadPointageNotifications(List<Notification> notifications,
                                           Map<Long, String> surveillantNameById,
                                           Map<Long, String> surveillantRoomById,
                                           Date limitDate) {
        supabaseClient.select("pointage", "*", "order=heure_pointage.desc&limit=150",
                new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(JsonArray pointageResult) {
                        try {
                            for (int i = 0; i < pointageResult.size(); i++) {
                                JsonObject pointageDoc = pointageResult.get(i).getAsJsonObject();

                                Long idSurveillant = pointageDoc.get("id_surveillant").getAsLong();
                                boolean retard = pointageDoc.get("retard").getAsBoolean();

                                String heurePointageStr = pointageDoc.get("heure_pointage").getAsString();
                                Date heurePointage;
                                try {
                                    heurePointage = DateUtils.parseSupabaseTimestamp(heurePointageStr);
                                } catch (ParseException e) {
                                    Log.w(TAG, "Failed to parse heure_pointage: " + heurePointageStr, e);
                                    continue;
                                }

                                String nomSurveillant = surveillantNameById.get(idSurveillant);
                                String numeroSalle = null;

                                if (surveillantRoomById.containsKey(idSurveillant)) {
                                    numeroSalle = surveillantRoomById.get(idSurveillant);
                                } else if (pointageDoc.has("numero_salle") && !pointageDoc.get("numero_salle").isJsonNull()) {
                                    String numeroSalleFromPointage = pointageDoc.get("numero_salle").getAsString();
                                    if (numeroSalleFromPointage != null && !numeroSalleFromPointage.trim().isEmpty()) {
                                        numeroSalle = numeroSalleFromPointage.trim();
                                        surveillantRoomById.put(idSurveillant, numeroSalle);
                                    }
                                }

                                // Filtrer les notifications de plus de 60 jours
                                if (nomSurveillant != null && heurePointage.after(limitDate)) {
                                    String notificationType = retard ? "retard" : "presence";

                                    Notification notification = new Notification(
                                            pointageDoc.has("id_pointage") ? pointageDoc.get("id_pointage").getAsLong() : null,
                                            nomSurveillant,
                                            notificationType,
                                            heurePointage,
                                            numeroSalle
                                    );
                                    notifications.add(notification);
                                }
                            }

                            // 3️⃣ Récupérer les notifications d'absences
                            loadAbsenceNotifications(notifications, surveillantNameById, surveillantRoomById, limitDate);

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing pointages", e);
                            isLoading = false;
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error loading pointages", error);
                        isLoading = false;
                    }
                });
    }
    




    private Calendar getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
    //convertir le date de planning et le temps optionnel en date complet pour determiner la session
    private Date convertToSessionTimestamp(String dateExamenStr, String heureDebutStr) throws ParseException {
        if (heureDebutStr != null) {
            heureDebutStr = heureDebutStr.trim();
        }
        if (dateExamenStr != null) {
            dateExamenStr = dateExamenStr.trim();
        }

        //si l'huere de debut est deja un timestamp, essayez d'utiliser DataUtils directement
        if (heureDebutStr != null && !heureDebutStr.isEmpty()) {
            // Heuristic: ISO-like timestamp often contains 'T'
            if (heureDebutStr.contains("T")) {
                try {
                    return DateUtils.parseSupabaseTimestamp(heureDebutStr);
                } catch (ParseException ignored) {
                    // Fallback to combine below
                }
            }

            // si c'est une heure en chaine de caractere comme HH:mm ou HH:mm:ss , combinez avec la date
            String timePart = heureDebutStr;
            if (timePart.matches("^\\d{2}:\\d{2}$")) {
                timePart = timePart + ":00"; // add seconds
            }
            if (timePart.matches("^\\d{2}:\\d{2}:\\d{2}$") && dateExamenStr != null && !dateExamenStr.isEmpty()) {
                String combined = dateExamenStr + " " + timePart; // yyyy-MM-dd HH:mm:ss
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setLenient(false);
                return sdf.parse(combined);
            }
        }

        // si la date de l'examen lui-meme est un timestamp complet
        if (dateExamenStr != null && dateExamenStr.contains("T")) {
            return DateUtils.parseSupabaseTimestamp(dateExamenStr);
        }

        // dernier recours : convertir seulemt la date , en admenttant 08:00:00
        if (dateExamenStr != null && !dateExamenStr.isEmpty()) {
            String combined = dateExamenStr + " 08:00:00";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setLenient(false);
            return sdf.parse(combined);
        }

        throw new ParseException("Invalid date/time for session reference", 0);
    }

    // Remove duplicates keeping the latest notification for the same surveillant/type/day/room
    private List<Notification> deduplicateNotifications(List<Notification> input) {
        Map<String, Notification> byKey = new LinkedHashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        dayFormat.setTimeZone(java.util.TimeZone.getTimeZone("Indian/Antananarivo"));
        for (Notification n : input) {
            if (n == null || n.getDateHeure() == null) continue;
            String day = dayFormat.format(n.getDateHeure());
            String room = n.getNumeroSalle() != null ? n.getNumeroSalle().trim().toUpperCase() : "";
            String type = n.getType() != null ? n.getType().toLowerCase() : "";
            String name = n.getNomSurveillant() != null ? n.getNomSurveillant() : "";
            // Include session to differentiate morning vs afternoon absences on the same day
            String session = n.getSession() != null ? n.getSession().trim().toUpperCase() : "";
            String key = name + "|" + type + "|" + day + "|" + room + "|" + session;

            Notification existing = byKey.get(key);
            if (existing == null || n.getDateHeure().after(existing.getDateHeure())) {
                byKey.put(key, n);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private void loadAbsenceNotifications(List<Notification> notifications,
                                          Map<Long, String> surveillantNameById,
                                          Map<Long, String> surveillantRoomById,
                                          Date limitDate) {
        // Charger les absences depuis la table sanction uniquement si l'examen est passé
        String limitDateStr = DateUtils.formatDateOnly(limitDate);
        String filter = "type=eq.ABSENCE&date_examen=gte." + limitDateStr;
        supabaseClient.select("sanction", "*", filter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray sanctionResult) {
                try {
                    Date now = new Date();
                    for (int i = 0; i < sanctionResult.size(); i++) {
                        JsonObject s = sanctionResult.get(i).getAsJsonObject();
                        // Vérifier que la date de l'examen est passée (sécurité supplémentaire)
                        String dateExamenStr = s.has("date_examen") && !s.get("date_examen").isJsonNull() ? s.get("date_examen").getAsString() : null;
                        if (dateExamenStr == null) continue;
                        Date examDate;
                        try {
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                            df.setLenient(false);
                            df.setTimeZone(java.util.TimeZone.getTimeZone("Indian/Antananarivo"));
                            examDate = df.parse(dateExamenStr);
                        } catch (ParseException e) {
                            continue;
                        }
                        if (examDate.after(now)) continue; // Ne pas notifier pour les examens futurs

                        String nom = s.has("nom_surveillant") && !s.get("nom_surveillant").isJsonNull() ? s.get("nom_surveillant").getAsString() : null;
                        if (nom == null && s.has("id_surveillant") && !s.get("id_surveillant").isJsonNull()) {
                            Long id = s.get("id_surveillant").getAsLong();
                            nom = surveillantNameById.get(id);
                        }
                        String room = s.has("numero_salle") && !s.get("numero_salle").isJsonNull() ? s.get("numero_salle").getAsString() : null;
                        String session = s.has("session") && !s.get("session").isJsonNull() ? s.get("session").getAsString() : null;

                        // Pour les absences : utiliser la date d'examen (pas la date de création) pour l'affichage
                        // Cela montre la date du jour où l'absence a eu lieu, pas le moment où elle a été détectée
                        // Toujours utiliser la date d'examen pour les absences, positionnée à midi en fuseau local pour éviter tout glissement
                        java.util.Calendar noonCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Indian/Antananarivo"));
                        noonCal.setTime(examDate);
                        noonCal.set(java.util.Calendar.HOUR_OF_DAY, 12);
                        noonCal.set(java.util.Calendar.MINUTE, 0);
                        noonCal.set(java.util.Calendar.SECOND, 0);
                        noonCal.set(java.util.Calendar.MILLISECOND, 0);
                        Date notifDate = noonCal.getTime();

                        if (nom != null) {
                            notifications.add(new Notification(null, nom, "absence", notifDate, room, session));
                        }
                    }
                    finalizeNotificationUpdate(deduplicateNotifications(notifications));
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du chargement des absences depuis sanction", e);
                    finalizeNotificationUpdate(deduplicateNotifications(notifications));
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Erreur lors du chargement des sanctions", error);
                finalizeNotificationUpdate(deduplicateNotifications(notifications));
            }
        });
    }
    
    private void finalizeNotificationUpdate(List<Notification> notifications) {
        // Trier les notifications par date décroissante
        Collections.sort(notifications, (a, b) -> b.getDateHeure().compareTo(a.getDateHeure()));

        // Garder seulement les 60 dernières notifications
        if (notifications.size() > 60) {
            notifications.subList(60, notifications.size()).clear();
        }

        // Envoyer les notifications externes pour les nouvelles notifications
        if (context != null) {
            for (Notification notif : notifications) {
                if (!isNotificationAlreadySeen(notif)) {
                    //sendExternalNotification(notif);
                }
            }
        }

        // Mettre à jour la liste des notifications précédentes
        previousNotifications = new ArrayList<>(notifications);

        // Mettre à jour les données LiveData
        notificationsLiveData.postValue(notifications);
        isLoading = false;
    }

    /**
     * Vérifier si une notification a déjà été vue (en mémoire ou en SharedPreferences)
     */
    private boolean isNotificationAlreadySeen(Notification notif) {
        // Vérifier en mémoire d'abord
        for (Notification prev : previousNotifications) {
            if (prev.getNomSurveillant() != null && prev.getNomSurveillant().equals(notif.getNomSurveillant())
                    && prev.getType() != null && prev.getType().equals(notif.getType())
                    && prev.getDateHeure() != null && notif.getDateHeure() != null
                    && prev.getDateHeure().getTime() == notif.getDateHeure().getTime()
                    && ((prev.getNumeroSalle() == null && notif.getNumeroSalle() == null)
                    || (prev.getNumeroSalle() != null && prev.getNumeroSalle().equals(notif.getNumeroSalle())))) {
                return true;
            }
        }
        
        // Vérifier en SharedPreferences (pour les redémarrages)
        String notificationKey = buildNotificationKey(notif);
        return notifiedNotifications.contains(notificationKey);
    }

    /**
     * Construire une clé unique pour une notification
     */
    private String buildNotificationKey(Notification notif) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Indian/Antananarivo"));
        String date = notif.getDateHeure() != null ? dateFormat.format(notif.getDateHeure()) : "unknown";
        String surveillance = notif.getNomSurveillant() != null ? notif.getNomSurveillant() : "unknown";
        String type = notif.getType() != null ? notif.getType() : "unknown";
        String room = notif.getNumeroSalle() != null ? notif.getNumeroSalle() : "none";
        String session = notif.getSession() != null ? notif.getSession() : "";
        return surveillance + "_" + type + "_" + date + "_" + room + "_" + session;
    }

    /**
     * Marquer une notification comme déjà vue (persister dans SharedPreferences)
     */
    private void markNotificationAsNotified(Notification notif) {
        if (sharedPreferences != null) {
            String notificationKey = buildNotificationKey(notif);
            notifiedNotifications.add(notificationKey);
            sharedPreferences.edit()
                    .putStringSet(NOTIFIED_NOTIFICATIONS_KEY, notifiedNotifications)
                    .apply();
            Log.d(TAG, "Notification marked as notified: " + notificationKey);
        }
    }

    /**
     * Envoyer une notification système externe
     */
    private void sendExternalNotification(Notification notif) {
        try {
            Long idSurveillant = notif.getIdSurveillant();
            String nomSurveillant = notif.getNomSurveillant();
            String numeroSalle = notif.getNumeroSalle();
            String type = notif.getType();
            
            // Formater la date au format "yyyy-MM-dd"
            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) {{
                        setTimeZone(java.util.TimeZone.getTimeZone("Indian/Antananarivo"));
                    }}
                    .format(notif.getDateHeure());

            if ("absence".equalsIgnoreCase(type)) {
                NotificationHelper.showAbsenceNotification(context, idSurveillant, nomSurveillant, numeroSalle, notif.getSession(), dateStr);
            } else if ("retard".equalsIgnoreCase(type)) {
                NotificationHelper.showRetardNotification(context, idSurveillant, nomSurveillant, numeroSalle, dateStr);
            } else if ("presence".equalsIgnoreCase(type)) {
                NotificationHelper.showPresenceNotification(context, idSurveillant, nomSurveillant, numeroSalle, dateStr);
            }
            
            // Marquer comme notifiée pour éviter les renvois
            markNotificationAsNotified(notif);
            Log.d(TAG, "Notification externe envoyée: " + type + " pour " + nomSurveillant);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi de la notification externe", e);
        }
    }
    /**
     * Supprime les notifications de plus de 60 jours de la base de données
     */
    private void deleteOldNotifications() {
        Calendar limitCal = Calendar.getInstance();
        limitCal.add(Calendar.DAY_OF_MONTH, -60);
        String limitDateStr = DateUtils.formatForSupabase(limitCal.getTime());
        
        Log.d(TAG, "Suppression des notifications antérieures à: " + limitDateStr);
        
        // Supprimer les anciens pointages (plus de 60 jours)
        String pointageFilter = "heure_pointage=lt." + limitDateStr;
        supabaseClient.delete("pointage", pointageFilter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Log.d(TAG, "Anciens pointages supprimés avec succès");
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Erreur lors de la suppression des anciens pointages", error);
            }
        });
    }

    /**
     * Vérifie les absences pour les examens déjà passés via HistoriqueViewModel
     * Un surveillant ne peut être absent que pour un examen déjà terminé
     */
    private void checkPassedExamAbsences() {
        try {
            HistoriqueViewModel historiqueViewModel = new HistoriqueViewModel();
            historiqueViewModel.checkSessionAbsences();
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Failed to initialize HistoriqueViewModel for absence check", e);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Arrêter l'auto-refresh
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
