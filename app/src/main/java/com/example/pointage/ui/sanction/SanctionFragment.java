package com.example.pointage.ui.sanction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;
import com.example.pointage.databinding.FragmentSanctionBinding;
import com.example.pointage.utils.PdfGenerator;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;

import com.example.pointage.ui.historique.HistoriqueViewModel;
import com.example.pointage.ui.historique.Pointage;
import com.example.pointage.ui.surveillant.DateRangeDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.DateFormatSymbols;

public class SanctionFragment extends Fragment {

    private FragmentSanctionBinding binding;
    private SanctionViewModel sanctionViewModel;
    private HistoriqueViewModel historiqueViewModel;
    private SanctionAdapter sanctionAdapter;
    private Spinner spinnerMonth, spinnerYear;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sanctionViewModel = new ViewModelProvider(this).get(SanctionViewModel.class);
        historiqueViewModel = new ViewModelProvider(requireActivity()).get(HistoriqueViewModel.class);
        // Initialiser SharedPreferences pour la persistance des sanctions notifiées
        if (getContext() != null) {
            sanctionViewModel.initializeSharedPreferences(getContext());
        }
        binding = FragmentSanctionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewSanction;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sanctionAdapter = new SanctionAdapter(new ArrayList<>());
        recyclerView.setAdapter(sanctionAdapter);

        spinnerMonth = binding.spinnerMonth;
        spinnerYear = binding.spinnerYear;

        // le spinner du mois
        String[] months = new DateFormatSymbols().getMonths();
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // commencer avec l'annee en cours -5 ans
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear; i++) { // Past 5 years and current year
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        //les listeners pour les spinners
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedMonth = spinnerMonth.getSelectedItemPosition();
                int selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());
                sanctionViewModel.loadSanctions(selectedMonth, selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ne rien faire
            }
        };

        spinnerMonth.setOnItemSelectedListener(spinnerListener);
        spinnerYear.setOnItemSelectedListener(spinnerListener);

        // Selectionner le mois et l'annee en cours
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));
        spinnerYear.setSelection(years.indexOf(String.valueOf(currentYear)));

        sanctionViewModel.getSanctions().observe(getViewLifecycleOwner(), sanctionList -> {
            if (sanctionList != null) {
                sanctionAdapter.setSanctionList(sanctionList);
            }
        });

        // Gestion du clic sur le bouton d'export PDF
        binding.btnExportPdf.setOnClickListener(v -> {
            // Afficher le dialogue de sélection de la période
            DateRangeDialog dialog = DateRangeDialog.newInstance((year, month) -> {
                // Cette méthode est appelée lorsque l'utilisateur a sélectionné une période
                generatePdfForPeriod(year, month);
            });
            
            // Afficher le dialogue
            dialog.show(getParentFragmentManager(), "date_range_dialog");
        });

        return root;
    }

    private void generatePdfForPeriod(int year, int month) {
        // Afficher un message de chargement
        String monthName = new DateFormatSymbols().getMonths()[month];
        Toast.makeText(requireContext(), 
            "Chargement des données pour " + monthName + " " + year + "...", 
            Toast.LENGTH_SHORT).show();
        
        // Définir la période de filtrage dans le ViewModel
        historiqueViewModel.setYear(year);
        historiqueViewModel.setMonth(month);
        
        // Forcer le rechargement des données avant de générer le PDF
        sanctionViewModel.loadSanctions(month, year);
        
        // S'abonner aux changements de données
        sanctionViewModel.getSanctions().observe(getViewLifecycleOwner(), sanctionList -> {
            if (sanctionList == null) return;
            
            // Une fois les données chargées, générer le PDF
            generatePdfWithData(year, month, monthName, sanctionList);
            
            // Ne pas oublier de retirer l'observateur pour éviter les fuites de mémoire
            sanctionViewModel.getSanctions().removeObservers(getViewLifecycleOwner());
        });
    }
    
    private void generatePdfWithData(int year, int month, String monthName, List<SurveillantSanction> sanctions) {
        Toast.makeText(requireContext(), 
            "Génération du rapport pour " + monthName + " " + year + "...", 
            Toast.LENGTH_SHORT).show();
        
        // Générer le rapport PDF avec les données mises à jour
        PdfGenerator pdfGenerator = new PdfGenerator(requireContext());
        pdfGenerator.generateGlobalReport(month, year, new PdfGenerator.OnGlobalPdfGeneratedListener() {
            @Override
            public void onPdfGenerated(Uri pdfUri) {
                // Le PDF a été généré avec succès
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Afficher un message de succès avec la période
                        Toast.makeText(getContext(), 
                            "Rapport pour " + monthName + " " + year + " généré avec succès", 
                            Toast.LENGTH_LONG).show();
                        
                        // Ouvrir le fichier PDF
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(pdfUri, "application/pdf");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Ouvrir avec"));
                        } catch (Exception e) {
                            Toast.makeText(getContext(), 
                                "Aucune application pour ouvrir le PDF n'a été trouvée", 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Gérer les erreurs
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), 
                            "Erreur lors de la génération du rapport: " + errorMessage, 
                            Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}