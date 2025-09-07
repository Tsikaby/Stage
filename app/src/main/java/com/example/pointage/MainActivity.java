package com.example.pointage;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.example.pointage.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseFirestore db;

    // QR scanner
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
                    // Découper le texte scanné ligne par ligne
                    String[] lines = scannedContent.split("\n");
                    String idSurveillantStr = lines[0].split(":")[1].trim();
                    String nomSurveillant = lines[1].split(":")[1].trim();
                    String contact = lines[2].split(":")[1].trim();
                    String idSalleStr = lines[3].split(":")[1].trim();

                    long idSurveillant = Long.parseLong(idSurveillantStr);
                    int idSalle = Integer.parseInt(idSalleStr);

                    Log.d("QR_SCAN", "ID Surveillant: " + idSurveillant + " | Nom: " + nomSurveillant);

                    // Vérifier le retard
                    Calendar calendar = Calendar.getInstance();
                    boolean isLate = calendar.get(Calendar.HOUR_OF_DAY) >= 8;

                    db = FirebaseFirestore.getInstance();

                    // Vérifier si un examen est prévu aujourd’hui
                    Timestamp startTimestamp = new Timestamp(getTodayStart());
                    Timestamp endTimestamp = new Timestamp(getTodayEnd());

                    db.collection("examen")
                            .whereGreaterThanOrEqualTo("date_examen", startTimestamp)
                            .whereLessThanOrEqualTo("date_examen", endTimestamp)
                            .get()
                            .addOnSuccessListener(task -> {
                                if (!task.isEmpty()) {
                                    // Sauvegarder le pointage
                                    Map<String, Object> pointageData = new HashMap<>();
                                    pointageData.put("id_pointage", System.currentTimeMillis()); // ✅ ID numérique Long
                                    pointageData.put("heure_pointage", new Timestamp(new Date()));
                                    pointageData.put("id_surveillant", idSurveillant);
                                    pointageData.put("nom_surveillant", nomSurveillant);
                                    pointageData.put("contact", contact);
                                    pointageData.put("id_salle", idSalle);
                                    pointageData.put("retard", isLate);

                                    db.collection("pointage").add(pointageData)
                                            .addOnSuccessListener(ref -> Snackbar.make(binding.getRoot(),
                                                    "Pointage OK pour " + nomSurveillant + ". Retard: " + isLate,
                                                    Snackbar.LENGTH_LONG).setAnchorView(R.id.fab).show())
                                            .addOnFailureListener(e -> Snackbar.make(binding.getRoot(),
                                                    "Erreur sauvegarde: " + e.getMessage(),
                                                    Snackbar.LENGTH_LONG).setAnchorView(R.id.fab).show());
                                } else {
                                    Snackbar.make(binding.getRoot(), "Pas d'examen prévu aujourd'hui", Snackbar.LENGTH_LONG)
                                            .setAnchorView(R.id.fab).show();
                                }
                            })
                            .addOnFailureListener(e -> Snackbar.make(binding.getRoot(),
                                    "Erreur récupération examen: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).setAnchorView(R.id.fab).show());

                } catch (Exception e) {
                    Snackbar.make(binding.getRoot(), "QR Code invalide: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).setAnchorView(R.id.fab).show();
                    e.printStackTrace();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // Bouton scan QR
        binding.appBarMain.fab.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Alignez le QR code dans le rectangle");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setOrientationLocked(true); // portrait uniquement
            options.setBarcodeImageEnabled(true);
            options.setCaptureActivity(CaptureAct.class);
            barcodeLauncher.launch(options);
        });

        // Navigation Drawer
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_surveillant, R.id.nav_historique, R.id.nav_sanction)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // Utilitaires pour la journée en cours
    private Date getTodayStart() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime();
    }

    private Date getTodayEnd() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime();
    }
}
