package com.example.pointage;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;

import com.example.pointage.databinding.ActivityMainBinding;
import com.example.pointage.ui.historique.HistoriqueViewModel;
import com.example.pointage.utils.EmailUtility;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private ConnectClient connectClient;
    private HistoriqueViewModel historiqueViewModel;

    // Launcher pour demander la permission POST_NOTIFICATIONS
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "Permission POST_NOTIFICATIONS accordée");
                } else {
                    Log.w("MainActivity", "Permission POST_NOTIFICATIONS refusée");
                }
            });

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
            connectClient = ConnectClient.getInstance();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        // Demander la permission POST_NOTIFICATIONS (Android 13+)
        requestNotificationPermission();

        // Initialisation du ViewModel
        historiqueViewModel = new ViewModelProvider(this).get(HistoriqueViewModel.class);

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

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_pending, R.id.nav_surveillant, R.id.nav_historique, R.id.nav_sanction)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Vérifier le rôle de l'utilisateur et afficher le menu admin si nécessaire
        checkUserRoleAndShowAdminMenu(navigationView);

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
        // Déconnexion : mettre log=false  et nettoyer la session locale
        String username = getSharedPreferences("login", MODE_PRIVATE).getString("username", null);

        if (username != null) {
            JsonObject body = new JsonObject();
            body.addProperty("log", false);
            String filter = "username=eq." + username;

            connectClient.update("utilisateurs", filter, body, new ConnectClient.ClientCallback() {
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
        } else if (item.getItemId() == R.id.action_check_absences) {
            forceAbsenceCheck();
            return true;
        } else if (item.getItemId() == R.id.action_test_email) {
            testEmailSending();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void forceAbsenceCheck() {
        new AlertDialog.Builder(this)
                .setTitle("Vérification d'absence")
                .setMessage("Déclencher manuellement la vérification des absences ?\n\nCela va vérifier tous les examens passés et marquer comme absents les surveillants qui n'ont pas pointé.")
                .setPositiveButton("Oui", (dialog, which) -> {
                    historiqueViewModel.forceCheckAbsences();
                    Snackbar.make(binding.getRoot(), "Vérification des absences déclenchée. Consultez les logs pour plus de détails.", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                })
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void testEmailSending() {
        new AlertDialog.Builder(this)
                .setTitle("Test Email")
                .setMessage("Envoyer un email de test à jeanbaptiste45522@gmail.com ?")
                .setPositiveButton("Envoyer", (dialog, which) -> {
                    String testEmail = "jeanbaptiste45522@gmail.com";
                    EmailUtility.sendTestEmail(testEmail);
                    Snackbar.make(binding.getRoot(), "Email de test envoyé à " + testEmail + ". Vérifiez votre boîte mail.", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab).show();
                    Log.i("MainActivity", "Test email envoyé à " + testEmail);
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkUserRoleAndShowAdminMenu(NavigationView navigationView) {
        String username = getSharedPreferences("login", MODE_PRIVATE).getString("username", null);
        if (username != null) {
            String filter = "username=eq." + username;
            connectClient.select("utilisateurs", "role", filter, new ConnectClient.ClientCallback() {
                @Override
                public void onSuccess(JsonArray result) {
                    if (result.size() > 0) {
                        String role = result.get(0).getAsJsonObject().get("role").getAsString();
                        if ("admin".equals(role)) {
                            // Afficher le menu de gestion des utilisateurs pour les admins
                            navigationView.getMenu().findItem(R.id.nav_user_management).setVisible(true);
                        }
                    }
                }

                @Override
                public void onError(Exception error) {
                    // En cas d'erreur, ne pas afficher le menu admin
                }
            });
        }
    }

    /**
     * Demander la permission POST_NOTIFICATIONS (Android 13+)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33+)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission non accordée, la demander
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        // Sur Android 12 et avant, aucune demande de permission nécessaire
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}