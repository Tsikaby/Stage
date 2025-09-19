package com.example.pointage;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;

public class TestSupabaseConnection {
    private static final String TAG = "TestSupabase";
    
    public static void testConnection() throws UnknownHostException {
        Log.d(TAG, "🔍 Début du test de connexion Supabase...");
        
        SupabaseClient client = SupabaseClient.getInstance();
        
        // Test 1: Récupérer les surveillants
        Log.d(TAG, "📋 Test 1: Récupération des surveillants...");
        client.select("surveillant", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) throws UnknownHostException {
                Log.d(TAG, "✅ Test 1 RÉUSSI: " + result.size() + " surveillants trouvés");
                for (int i = 0; i < result.size(); i++) {
                    JsonObject surveillant = result.get(i).getAsJsonObject();
                    Log.d(TAG, "👤 Surveillant " + (i+1) + ": " + 
                          surveillant.get("nom_surveillant").getAsString());
                }
                
                // Test 2: Récupérer les pointages
                testPointages();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Test 1 ÉCHOUÉ: " + error.getMessage());
            }
        });
    }
    
    private static void testPointages() throws UnknownHostException {
        Log.d(TAG, "📋 Test 2: Récupération des pointages...");
        
        SupabaseClient client = SupabaseClient.getInstance();
        client.select("pointage", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) throws UnknownHostException {
                Log.d(TAG, "✅ Test 2 RÉUSSI: " + result.size() + " pointages trouvés");
                for (int i = 0; i < result.size(); i++) {
                    JsonObject pointage = result.get(i).getAsJsonObject();
                    Log.d(TAG, "📝 Pointage " + (i+1) + ": ID=" + 
                          pointage.get("id_pointage").getAsLong() + 
                          ", Surveillant=" + pointage.get("id_surveillant").getAsLong());
                }
                
                // Test 3: Insérer un pointage de test
                testInsertPointage();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Test 2 ÉCHOUÉ: " + error.getMessage());
            }
        });
    }
    
    private static void testInsertPointage() throws UnknownHostException {
        Log.d(TAG, "📋 Test 3: Insertion d'un pointage de test...");
        
        SupabaseClient client = SupabaseClient.getInstance();
        JsonObject testPointage = new JsonObject();
        testPointage.addProperty("id_surveillant", 999);
        testPointage.addProperty("heure_pointage", new java.util.Date().toString());
        testPointage.addProperty("retard", false);
        testPointage.addProperty("date_pointage", new java.util.Date().toString().split(" ")[0]);
        
        client.insert("pointage", testPointage, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Log.d(TAG, "✅ Test 3 RÉUSSI: Pointage de test inséré");
                Log.d(TAG, "🎉 TOUS LES TESTS SONT PASSÉS - CONNEXION SUPABASE OK!");
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Test 3 ÉCHOUÉ: " + error.getMessage());
            }
        });
    }
}

