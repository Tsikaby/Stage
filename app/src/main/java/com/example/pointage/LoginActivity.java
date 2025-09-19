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

import com.example.pointage.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.net.UnknownHostException;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private SupabaseClient supabaseClient;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier si l'utilisateur est déjà connecté
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        if (prefs.getBoolean("log", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        try {
            supabaseClient = SupabaseClient.getInstance();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        mainHandler = new Handler(Looper.getMainLooper());

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Encodage des valeurs pour éviter les erreurs avec caractères spéciaux
        String encodedUsername;
        String encodedPassword;
        try {
            encodedUsername = URLEncoder.encode(username, "UTF-8");
            encodedPassword = URLEncoder.encode(password, "UTF-8");
        } catch (Exception e) {
            encodedUsername = username;
            encodedPassword = password;
        }

        String filter = "username=eq." + encodedUsername + "&mdp=eq." + encodedPassword;
        System.out.println("Requête filter: " + filter); // Pour déboguer

        supabaseClient.select("utilisateurs", "*", filter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() > 0) {
                    // Connexion réussie -> mettre log=true côté base puis poursuivre
                    String encodedUsername;
                    try {
                        encodedUsername = URLEncoder.encode(username, "UTF-8");
                    } catch (Exception e) {
                        encodedUsername = username;
                    }

                    JsonObject body = new JsonObject();
                    body.addProperty("log", true);
                    String updateFilter = "username=eq." + encodedUsername;

                    supabaseClient.update("utilisateurs", updateFilter, body, new SupabaseClient.SupabaseCallback() {
                        @Override
                        public void onSuccess(JsonArray updateResult) {
                            // Persistance locale
                            getSharedPreferences("login", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("log", true)
                                    .putString("username", username)
                                    .apply();

                            Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();

                            // Ouvrir MainActivity
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(Exception error) {
                            // Même si la MAJ serveur échoue, on autorise l'accès et on notifie
                            Toast.makeText(LoginActivity.this, "Maj log=true échouée: " + error.getMessage(), Toast.LENGTH_SHORT).show();

                            getSharedPreferences("login", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("log", true)
                                    .putString("username", username)
                                    .apply();

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                } else {
                    Toast.makeText(LoginActivity.this, "Nom d'utilisateur ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(LoginActivity.this, "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                error.printStackTrace(); // Pour voir les détails de l'erreur
            }
        });
    }
}