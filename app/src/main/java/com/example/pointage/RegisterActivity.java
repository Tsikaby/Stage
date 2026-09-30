package com.example.pointage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnSubmit;
    private ConnectClient connectClient;
    private Handler mainHandler;
    private boolean isApproved = false;
    private boolean isRejected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier si l'utilisateur est déjà en attente d'approbation
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String pendingUsername = prefs.getString("pending_username", null);
        boolean isPending = prefs.getBoolean("is_pending", false);
        
        if (isPending && pendingUsername != null) {
            // L'utilisateur est en attente, initialiser les composants nécessaires
            try {
                connectClient = ConnectClient.getInstance();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            mainHandler = new Handler(Looper.getMainLooper());
            
            // Afficher l'écran d'attente
            showWaitingScreen();
            edtUsername = new EditText(this); // Créer un EditText temporaire pour stocker le nom
            edtUsername.setText(pendingUsername);
            return;
        }
        
        setContentView(R.layout.activity_register);

        edtUsername = findViewById(R.id.edtRegUsername);
        edtPassword = findViewById(R.id.edtRegPassword);
        btnSubmit = findViewById(R.id.btnRegisterSubmit);

        try {
            connectClient = ConnectClient.getInstance();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        mainHandler = new Handler(Looper.getMainLooper());

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void submitRegistration() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("mdp", password);
        body.addProperty("approved", false);
        body.addProperty("role", "user");

        connectClient.insert("utilisateurs", body, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                // Sauvegarder l'état d'attente
                getSharedPreferences("login", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_pending", true)
                        .putString("pending_username", username)
                        .apply();
                
                // Envoyer une notification aux admins
                notifyAdminsOfNewRegistration(username);
                // Afficher l'écran d'attente au lieu de fermer
                showWaitingScreen();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(RegisterActivity.this, "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyAdminsOfNewRegistration(String username) {
        // Créer une notification pour les admins
        JsonObject notification = new JsonObject();
        notification.addProperty("message", "Nouvelle demande d'inscription de: " + username);
        notification.addProperty("type", "registration");
        notification.addProperty("username", username);
        notification.addProperty("timestamp", System.currentTimeMillis());
        
        // Insérer la notification dans la base de données
        connectClient.insert("notifications", notification, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                // Notification envoyée avec succès
            }

            @Override
            public void onError(Exception error) {
                // En cas d'erreur, on continue quand même
            }
        });
    }

    private void showWaitingScreen() {
        try {
            setContentView(R.layout.activity_register_waiting);
            
            // Bouton pour retourner à la connexion
            findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
                clearPendingState();
                finish();
            });
            
            // Démarrer la vérification périodique du statut d'approbation
            startApprovalCheck();
        } catch (Exception e) {
            // En cas d'erreur, nettoyer l'état et rediriger vers la connexion
            clearPendingState();
            Toast.makeText(this, "Erreur d'affichage, retour à la connexion", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
    
    private void startApprovalCheck() {
        if (connectClient == null) {
            Toast.makeText(this, "Erreur d'initialisation, retour à la connexion", Toast.LENGTH_SHORT).show();
            clearPendingState();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable checkApproval = new Runnable() {
            @Override
            public void run() {
                if (!isApproved && !isRejected) {
                    checkUserApprovalStatus();
                    // Vérifier toutes les 3 secondes seulement si pas encore approuvé ou rejeté
                    handler.postDelayed(this, 3000);
                }
            }
        };
        handler.post(checkApproval);
    }
    
    private void checkUserApprovalStatus() {
        if (isApproved || isRejected) return; // Éviter les vérifications multiples
        
        if (edtUsername == null) {
            // Récupérer le nom d'utilisateur depuis les préférences
            SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
            String username = prefs.getString("pending_username", "");
            if (username.isEmpty()) {
                clearPendingState();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return;
            }
            checkUserApprovalStatusWithUsername(username);
            return;
        }
        
        String username = edtUsername.getText().toString().trim();
        checkUserApprovalStatusWithUsername(username);
    }
    
    private void checkUserApprovalStatusWithUsername(String username) {
        // Vérifier d'abord si l'utilisateur existe encore (pas rejeté)
        String userExistsFilter = "username=eq." + username;
        connectClient.select("utilisateurs", "approved", userExistsFilter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() == 0 && !isRejected) {
                    isRejected = true; // Marquer comme rejeté pour éviter les répétitions
                    // L'utilisateur n'existe plus (rejeté), rediriger vers la connexion
                    clearPendingState();
                    Toast.makeText(RegisterActivity.this, "Votre demande a été rejetée.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // L'utilisateur existe, vérifier s'il est approuvé
                    String approvedFilter = "username=eq." + username + "&approved=eq.true";
                    connectClient.select("utilisateurs", "approved", approvedFilter, new ConnectClient.ClientCallback() {
                        @Override
                        public void onSuccess(JsonArray approvedResult) {
                            if (approvedResult.size() > 0 && !isApproved) {
                                isApproved = true; // Marquer comme approuvé pour éviter les répétitions
                                clearPendingState();
                                // L'utilisateur a été approuvé, rediriger vers la page d'accueil
                                Toast.makeText(RegisterActivity.this, "Votre compte a été approuvé !", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            // En cas d'erreur, continuer à vérifier
                        }
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                // En cas d'erreur, continuer à vérifier
            }
        });
    }
    
    private void clearPendingState() {
        getSharedPreferences("login", MODE_PRIVATE)
                .edit()
                .remove("is_pending")
                .remove("pending_username")
                .apply();
    }
}



