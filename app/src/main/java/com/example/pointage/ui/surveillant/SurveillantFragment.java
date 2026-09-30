package com.example.pointage.ui.surveillant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;
import com.example.pointage.databinding.FragmentSurveillantBinding;
import com.example.pointage.ui.historique.HistoriqueViewModel;
import com.example.pointage.ui.historique.Pointage;
import com.example.pointage.utils.PdfGenerator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SurveillantFragment extends Fragment {

    private FragmentSurveillantBinding binding;
    private SurveillantViewModel surveillantViewModel;
    private SurveillantAdapter surveillantAdapter;
    private HistoriqueViewModel historiqueViewModel;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        surveillantViewModel = new ViewModelProvider(this).get(SurveillantViewModel.class);
        historiqueViewModel = new ViewModelProvider(requireActivity()).get(HistoriqueViewModel.class);
        binding = FragmentSurveillantBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewSurveillants;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        surveillantAdapter = new SurveillantAdapter(new ArrayList<>());
        surveillantAdapter.setOnGeneratePdfListener((idSurveillant, nomSurveillant) -> {
            generatePdfReport(idSurveillant, nomSurveillant);
        });
        recyclerView.setAdapter(surveillantAdapter);

        surveillantViewModel.getSurveillants().observe(getViewLifecycleOwner(), surveillants -> {
            if (surveillants != null) {
                surveillantAdapter.setSurveillantList(surveillants);
                // Arrêter l'animation de rafraîchissement
                binding.swipeRefreshSurveillants.setRefreshing(false);
            }
        });

        // Configuration du raffraichissement automatique
        binding.swipeRefreshSurveillants.setOnRefreshListener(() -> {
            // Recharger les surveillants
            surveillantViewModel.loadSurveillants();
        });

        // configuration du searchview
        binding.searchViewSurveillants.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                surveillantAdapter.filter(newText);
                return true;
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now proceed
                Toast.makeText(getContext(), "Storage permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(getContext(), "Storage permission denied. Cannot save QR code.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void generatePdfReport(int idSurveillant, String nomSurveillant) {
        // Afficher le dialogue de sélection de la période
        DateRangeDialog dialog = DateRangeDialog.newInstance((year, month) -> {
            // Cette méthode est appelée lorsque l'utilisateur a sélectionné une période
            generatePdfForPeriod(idSurveillant, nomSurveillant, year, month);
        });
        
        // Afficher le dialogue
        dialog.show(getParentFragmentManager(), "date_range_dialog");
    }
    
    private void generatePdfForPeriod(int idSurveillant, String nomSurveillant, int year, int month) {
        // Afficher un message de chargement
        Toast.makeText(requireContext(), "Génération du rapport pour " + getMonthName(month) + " " + year + "...", Toast.LENGTH_SHORT).show();
        
        // Définir la période de filtrage dans le ViewModel
        historiqueViewModel.setYear(year);
        historiqueViewModel.setMonth(month);
        
        // Charger les données historiques pour la période sélectionnée
        historiqueViewModel.getAllHistoriqueForPdf(new HistoriqueViewModel.OnHistoriqueLoadedListener() {
            @Override
            public void onHistoriqueLoaded(List<Pointage> pointages) {
                // Filtrer les pointages pour le surveillant et la période sélectionnés
                List<Pointage> filteredPointages = new ArrayList<>();
                Calendar cal = Calendar.getInstance();
                
                for (Pointage p : pointages) {
                    if (p.getId_surveillant() != null && p.getId_surveillant() == idSurveillant) {
                        cal.setTime(p.getHeure_pointage());
                        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                            filteredPointages.add(p);
                        }
                    }
                }
                
                if (filteredPointages.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Aucune donnée trouvée pour la période sélectionnée", Toast.LENGTH_LONG).show()
                        );
                    }
                    return;
                }
                
                // Générer le PDF avec les données filtrées
                PdfGenerator pdfGenerator = new PdfGenerator(requireContext());
                pdfGenerator.generateSurveillantReport(nomSurveillant, idSurveillant, filteredPointages, 
                    new PdfGenerator.OnPdfGeneratedListener() {
                        @Override
                        public void onPdfGenerated(Uri pdfUri, String nomSurveillant) {
                            // Le PDF a été généré avec succès
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Afficher un message de succès avec la période
                                    String period = getMonthName(month) + " " + year;
                                    Toast.makeText(getContext(), 
                                        "Rapport pour " + period + " généré avec succès", 
                                        Toast.LENGTH_LONG).show();
                                    
                                    // Ouvrir le fichier PDF
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setDataAndType(pdfUri, "application/pdf");
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(Intent.createChooser(intent, "Ouvrir avec"));
                                    } catch (Exception e) {
                                        Toast.makeText(getContext(), "Aucune application pour ouvrir le PDF n'a été trouvée", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Gérer les erreurs
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> 
                                    Toast.makeText(getContext(), "Erreur: " + errorMessage, Toast.LENGTH_LONG).show()
                                );
                            }
                        }
                    });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Erreur lors du chargement des données: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
    
    private String getMonthName(int month) {
        String[] months = getResources().getStringArray(R.array.months_array);
        if (month >= 0 && month < months.length) {
            return months[month];
        }
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
