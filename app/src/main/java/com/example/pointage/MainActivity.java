package com.example.pointage;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import com.example.pointage.databinding.ActivityMainBinding;
import com.example.pointage.ui.historique.HistoriqueViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.example.pointage.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.net.URLEncoder;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SupabaseClient supabaseClient;
    private HistoriqueViewModel historiqueViewModel;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Snackbar.make(binding.getRoot(), "Scan annulé", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                    return;
                }

                String scannedContent = result.getContents();
                Log.d("QR_SCAN", "Contenu scanné:\n" + scannedContent);

                try {
                    String[] lines = scannedContent.split("\n");
                    String idSurveillantStr = lines[0].split(":")[1].trim();
                    String nomSurveillant = lines[1].split(":")[1].trim();
                    String contact = lines[2].split(":")[1].trim();
                    String numeroSalleStr = lines[3].split(":")[1].trim();

                    long idSurveillant = Long.parseLong(idSurveillantStr);

                    Log.d("QR_SCAN", "ID Surveillant: " + idSurveillant + " | Nom: " + nomSurveillant + " | Salle: " + numeroSalleStr);

                    // Utiliser le ViewModel pour gérer le scan
                    historiqueViewModel.performScan(numeroSalleStr, idSurveillant, nomSurveillant,
                            new HistoriqueViewModel.OnScanResultListener() {
                                @Override
                                public void onScanSuccess(String message) {
                                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                                            .setAnchorView(R.id.fab).show();
                                }

                                @Override
                                public void onScanFailure(String errorMessage) {
                                    Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG)
                                            .setAnchorView(R.id.fab).show();
                                }
                            });

                } catch (ArrayIndexOutOfBoundsException e) {
                    Snackbar.make(binding.getRoot(), "Format QR Code invalide", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                    Log.e("QR_SCAN", "Format invalide", e);
                } catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), "ID surveillant invalide", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                    Log.e("QR_SCAN", "ID invalide", e);
                } catch (Exception e) {
                    Snackbar.make(binding.getRoot(), "Erreur de scan: " + e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                    Log.e("QR_SCAN", "Erreur générale", e);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            supabaseClient = SupabaseClient.getInstance();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        // Initialisation du ViewModel
        historiqueViewModel = new ViewModelProvider(this).get(HistoriqueViewModel.class);

        // Test de connexion Supabase
        try {
            TestSupabaseConnection.testConnection();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        setSupportActionBar(binding.appBarMain.toolbar);

        binding.appBarMain.fab.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Alignez le QR code dans le rectangle");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setBarcodeImageEnabled(true);
            options.setCaptureActivity(CaptureAct.class);
            barcodeLauncher.launch(options);
        });

        // Test de connexion Supabase au clic long sur le FAB
        binding.appBarMain.fab.setOnLongClickListener(view -> {
            Snackbar.make(binding.getRoot(), "Test de connexion Supabase...", Snackbar.LENGTH_SHORT).show();
            try {
                TestSupabaseConnection.testConnection();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            return true;
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_surveillant, R.id.nav_historique, R.id.nav_sanction)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Gérer le clic sur "Se déconnecter" dans le drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                confirmLogout();
                return true;
            }
            // Laisser le comportement par défaut pour les autres items
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (!handled) {
                navController.navigate(item.getItemId());
            }
            DrawerLayout drawer1 = binding.drawerLayout;
            drawer.closeDrawers();
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> performLogout())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        // Déconnexion : mettre log=false côté Supabase et nettoyer la session locale
        String username = getSharedPreferences("login", MODE_PRIVATE).getString("username", null);

        if (username != null) {
            String encodedUsername;
            try {
                encodedUsername = URLEncoder.encode(username, "UTF-8");
            } catch (Exception e) {
                encodedUsername = username;
            }

            JsonObject body = new JsonObject();
            body.addProperty("log", false);
            String filter = "username=eq." + encodedUsername;

            supabaseClient.update("utilisateurs", filter, body, new SupabaseClient.SupabaseCallback() {
                @Override
                public void onSuccess(JsonArray result) {
                    // Nettoyer local et retourner au login
                    getSharedPreferences("login", MODE_PRIVATE)
                            .edit()
                            .putBoolean("log", false)
                            .remove("username")
                            .apply();
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    // Même si l'UPDATE échoue, on nettoie localement et on retourne au login
                    getSharedPreferences("login", MODE_PRIVATE)
                            .edit()
                            .putBoolean("log", false)
                            .remove("username")
                            .apply();
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
            });
        } else {
            // Pas de username local : nettoyage simple
            getSharedPreferences("login", MODE_PRIVATE)
                    .edit()
                    .putBoolean("log", false)
                    .remove("username")
                    .apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}