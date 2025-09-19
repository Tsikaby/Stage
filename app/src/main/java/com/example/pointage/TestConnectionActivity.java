package com.example.pointage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;

public class TestConnectionActivity extends AppCompatActivity {

    private static final String TAG = "TestConnection";
    private TextView logTextView;
    private SupabaseClient client;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_connection);

        logTextView = findViewById(R.id.logTextView);
        try {
            client = SupabaseClient.getInstance();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        logMessage("🔍 Début des tests de connexion...");
        testSimpleConnection();
    }

    private void logMessage(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            logTextView.append(message + "\n\n");
        });
    }

    private void testSimpleConnection() {
        logMessage("📋 Test 1: Connexion simple à la table 'utilisateurs'");

        client.select("utilisateurs", "count", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                logMessage("✅ CONNEXION RÉUSSIE!");
                logMessage("Réponse: " + result.toString());
                testTableStructure();
            }

            @Override
            public void onError(Exception error) {
                logMessage("❌ ERREUR: " + error.getMessage());
                logMessage("Vérifiez:\n1. La connexion Internet\n2. La clé API Supabase\n3. Les politiques RLS");
            }
        });
    }

    private void testTableStructure() {
        logMessage("📋 Test 2: Structure de la table 'utilisateurs'");

        client.select("utilisateurs", "*", "limit=5", new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                logMessage("✅ Structure OK: " + result.size() + " enregistrements");
                for (int i = 0; i < result.size(); i++) {
                    JsonObject user = result.get(i).getAsJsonObject();
                    logMessage("Utilisateur " + (i+1) + ": " + user.toString());
                }
                testAuthentication();
            }

            @Override
            public void onError(Exception error) {
                logMessage("❌ Erreur structure: " + error.getMessage());
            }
        });
    }

    private void testAuthentication() {
        logMessage("📋 Test 3: Authentification avec jean/baptiste");

        String filter = "username=eq.jean&mdp=eq.baptiste";
        client.select("utilisateurs", "*", filter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() > 0) {
                    logMessage("✅ AUTHENTIFICATION RÉUSSIE!");
                    JsonObject user = result.get(0).getAsJsonObject();
                    logMessage("Utilisateur connecté: " + user.toString());
                } else {
                    logMessage("❌ Aucun utilisateur trouvé avec ces identifiants");
                }
            }

            @Override
            public void onError(Exception error) {
                logMessage("❌ Erreur authentification: " + error.getMessage());
            }
        });
    }
}