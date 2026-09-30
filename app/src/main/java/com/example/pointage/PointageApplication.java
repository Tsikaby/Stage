package com.example.pointage;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.pointage.utils.NotificationHelper;
import com.example.pointage.workers.NotificationSyncWorker;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class PointageApplication extends Application {
    private static final String TAG = "PointageApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialiser le helper de notifications
        NotificationHelper.initialize(this);
        
        // Enregistrer la tâche de synchronisation périodique
        scheduleNotificationSync();
        
        // Réinitialiser le statut log dans la base de données si l'app a été désinstallée
        try {
            checkAndResetLoginStatus();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enregistrer une tâche de synchronisation périodique (toutes les 15 minutes)
     */
    private void scheduleNotificationSync() {
        try {
            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest
                    .Builder(NotificationSyncWorker.class, 15, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "notification_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWorkRequest
            );
            
            Log.d(TAG, "✅ Tâche de synchronisation enregistrée (toutes les 15 minutes)");
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'enregistrement de la tâche de synchronisation", e);
        }
    }

    private void checkAndResetLoginStatus() throws UnknownHostException {
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        SharedPreferences appPrefs = getSharedPreferences("app_state", MODE_PRIVATE);
        
        // Vérifier si c'est la première exécution après installation
        boolean isFirstRun = appPrefs.getBoolean("first_run", true);
        
        if (isFirstRun) {
            // Première exécution après installation/réinstallation
            String savedUsername = prefs.getString("username", null);
            
            if (savedUsername != null) {
                // Un utilisateur était connecté avant la désinstallation
                // Mettre à jour le statut log=false dans la base de données
                ConnectClient connectClient = ConnectClient.getInstance();

                JsonObject body = new JsonObject();
                body.addProperty("log", false);
                String filter = "username=eq." + savedUsername;

                connectClient.update("utilisateurs", filter, body, new ConnectClient.ClientCallback() {
                    @Override
                    public void onSuccess(JsonArray result) {
                        // Statut mis à jour avec succès
                        System.out.println("Statut log réinitialisé pour l'utilisateur: " + savedUsername);
                    }

                    @Override
                    public void onError(Exception error) {
                        // Erreur lors de la mise à jour
                        System.err.println("Erreur lors de la réinitialisation du statut log: " + error.getMessage());
                    }
                });
            }
            
            // Nettoyer les préférences de connexion
            prefs.edit()
                    .putBoolean("log", false)
                    .remove("username")
                    .apply();
            
            // Marquer que ce n'est plus la première exécution
            appPrefs.edit()
                    .putBoolean("first_run", false)
                    .apply();
        }
    }
}
