package com.example.pointage.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.pointage.ConnectClient;
import com.example.pointage.utils.NotificationHelper;
import com.example.pointage.ui.historique.DateUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NotificationSyncWorker extends Worker {
    private static final String TAG = "NotificationSyncWorker";
    private static final String PREFS_NAME = "notification_sync";
    private static final String LAST_SYNC_KEY = "last_sync_timestamp";
    private static final String LAST_NOTIFICATION_KEY = "last_notification";

    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private void fetchRecentAbsences(String filter, SharedPreferences prefs) throws UnknownHostException {
        ConnectClient connectClient = ConnectClient.getInstance();

        final CountDownLatch latch = new CountDownLatch(1);

        connectClient.select("sanction", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject s = result.get(i).getAsJsonObject();
                        if (!s.has("type") || s.get("type").isJsonNull()) continue;
                        String type = s.get("type").getAsString();
                        if (!"ABSENCE".equalsIgnoreCase(type)) continue;

                        Long idSurveillant = s.has("id_surveillant") && !s.get("id_surveillant").isJsonNull()
                                ? s.get("id_surveillant").getAsLong() : null;
                        String nomSurveillant = s.has("nom_surveillant") && !s.get("nom_surveillant").isJsonNull()
                                ? s.get("nom_surveillant").getAsString() : null;
                        String numeroSalle = s.has("numero_salle") && !s.get("numero_salle").isJsonNull()
                                ? s.get("numero_salle").getAsString() : null;
                        String session = s.has("session") && !s.get("session").isJsonNull()
                                ? s.get("session").getAsString() : null;
                        String dateExamen = s.has("date_examen") && !s.get("date_examen").isJsonNull()
                                ? s.get("date_examen").getAsString() : null;

                        if (idSurveillant == null || dateExamen == null) continue;

                        if (nomSurveillant != null) {
                            NotificationHelper.showAbsenceNotification(getApplicationContext(), idSurveillant, nomSurveillant, numeroSalle, session, dateExamen);
                        } else {
                            // Récupérer le nom si absent dans la sanction
                            fetchSurveillantNameForAbsence(idSurveillant, numeroSalle, session, dateExamen);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du traitement des absences", e);
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de la récupération des sanctions ABSENCE", e);
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while waiting for absence sync", e);
        }
    }

    private void fetchSurveillantNameForAbsence(long idSurveillant, String numeroSalle, String session, String dateExamen) throws UnknownHostException {
        ConnectClient connectClient = ConnectClient.getInstance();
        String filter = "id_surveillant=eq." + idSurveillant;
        connectClient.select("surveillant", "nom_surveillant", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() > 0) {
                    String nomSurveillant = result.get(0).getAsJsonObject().get("nom_surveillant").getAsString();
                    NotificationHelper.showAbsenceNotification(getApplicationContext(), idSurveillant, nomSurveillant, numeroSalle, session, dateExamen);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de la récupération du surveillant (absence)", e);
            }
        });
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, " Synchronisation des notifications en arrière-plan...");
        try {
            syncNotificationsFromSupabase();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, " Erreur lors de la synchronisation", e);
            return Result.retry();
        }
    }

    private void syncNotificationsFromSupabase() throws UnknownHostException {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSyncTime = prefs.getLong(LAST_SYNC_KEY, 0);
        
        // Si c'est la première synchronisation, récupérer seulement les pointages des 24 dernières heures
        // pour éviter d'afficher des notifications pour des événements passés au redémarrage
        long syncThresholdTime = lastSyncTime > 0 ? lastSyncTime : System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        
        // Formater le timestamp pour Supabase (format ISO 8601)
        String lastSyncStr = formatTimestampForSupabase(syncThresholdTime);
        String pointageFilter = "heure_pointage=gt." + lastSyncStr;

        // 1) Récupérer les nouveaux pointages pour présence/retard
        fetchRecentPointages(pointageFilter, prefs);

        // 2) Récupérer les absences récentes (sanction type ABSENCE) depuis la dernière date (par jour)
        String lastSyncDateOnly = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date(syncThresholdTime));
        String sanctionFilter = "type=eq.ABSENCE&date_examen=gte." + lastSyncDateOnly;
        fetchRecentAbsences(sanctionFilter, prefs);
    }

    private void fetchRecentPointages(String filter, SharedPreferences prefs) throws UnknownHostException {
        ConnectClient connectClient = ConnectClient.getInstance();
        
        final CountDownLatch latch = new CountDownLatch(1);

        connectClient.select("pointage", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Log.d(TAG, " Récupération de " + result.size() + " pointages");
                
                if (result.size() > 0) {
                    // Traiter chaque pointage
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject pointage = result.get(i).getAsJsonObject();
                        processPointage(pointage, prefs);
                    }
                    
                    // Mettre à jour le timestamp de la dernière synchronisation
                    long currentTime = System.currentTimeMillis();
                    prefs.edit().putLong(LAST_SYNC_KEY, currentTime).apply();
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, " Erreur lors de la récupération des pointages", e);
                latch.countDown();
            }
        });

        // Attendre max 30 secondes
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while waiting for sync", e);
        }
    }

    private void processPointage(JsonObject pointage, SharedPreferences prefs) {
        try {
            long idSurveillant = pointage.get("id_surveillant").getAsLong();
            String numeroSalle = pointage.get("numero_salle").getAsString();
            boolean isRetard = pointage.get("retard").getAsBoolean();
            String heure = pointage.get("heure_pointage").getAsString();
            
            // Récupérer le nom du surveillant
            fetchSurveillantName(idSurveillant, numeroSalle, isRetard, heure, prefs);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement du pointage", e);
        }
    }

    private void fetchSurveillantName(long idSurveillant, String numeroSalle, boolean isRetard, String heure, SharedPreferences prefs) throws UnknownHostException {
        ConnectClient connectClient = ConnectClient.getInstance();
        String filter = "id_surveillant=eq." + idSurveillant;
        
        // Formater la date au format "yyyy-MM-dd" en utilisant un parseur tolérant aux fuseaux
        String dateStr = "";
        try {
            Date heureDate = DateUtils.parseSupabaseTimestamp(heure);
            SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateStr = targetFormat.format(heureDate);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du formatage de la date: " + heure, e);
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        }
        
        final String finalDateStr = dateStr;

        connectClient.select("surveillant", "nom_surveillant", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() > 0) {
                    String nomSurveillant = result.get(0).getAsJsonObject().get("nom_surveillant").getAsString();
                    
                    // Afficher la notification
                    if (isRetard) {
                        NotificationHelper.showRetardNotification(getApplicationContext(), idSurveillant, nomSurveillant, numeroSalle, finalDateStr);
                    } else {
                        NotificationHelper.showPresenceNotification(getApplicationContext(), idSurveillant, nomSurveillant, numeroSalle, finalDateStr);
                    }
                    
                    // Sauvegarder comme dernière notification
                    prefs.edit()
                            .putString(LAST_NOTIFICATION_KEY, nomSurveillant + " - " + numeroSalle + " (" + (isRetard ? "Retard" : "Présence") + ")")
                            .apply();
                    
                    Log.d(TAG, "🔔 Notification affichée: " + nomSurveillant);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de la récupération du surveillant", e);
            }
        });
    }

    private String formatTimestampForSupabase(long timestamp) {
        if (timestamp == 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}