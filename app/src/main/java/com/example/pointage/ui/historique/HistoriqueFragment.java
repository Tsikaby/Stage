package com.example.pointage.ui.historique;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pointage.databinding.FragmentHistoriqueBinding;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoriqueFragment extends Fragment implements HistoriqueAdapter.OnDeleteClickListener {

    private FragmentHistoriqueBinding binding;
    private HistoriqueViewModel historiqueViewModel;
    private HistoriqueAdapter historiqueAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoriqueBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        historiqueViewModel = new ViewModelProvider(this).get(HistoriqueViewModel.class);

        // --- RecyclerView ---
        historiqueAdapter = new HistoriqueAdapter(new ArrayList<>(), this);
        binding.recyclerViewHistorique.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewHistorique.setAdapter(historiqueAdapter);

        // --- Observer ---
        historiqueViewModel.getHistorique().observe(getViewLifecycleOwner(), historiqueList -> {
            if (historiqueList != null) {
                historiqueAdapter.setHistoriqueList(historiqueList);
            }
        });

        // --- Spinner pour filtrer le mois ---
        setupMonthSpinner();

        // --- Spinner pour filtrer l'année ---
        setupYearSpinner();

        return root;
    }

    private void setupMonthSpinner() {
        Spinner spinner = binding.spinnerMonth;
        String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Initialiser sur le mois actuel
        spinner.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                historiqueViewModel.setMonth(position); // Filtrer par mois
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupYearSpinner() {
        Spinner spinner = binding.spinnerYear;
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 2020; i <= currentYear; i++) {
            years.add(String.valueOf(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Initialiser sur l'année actuelle
        spinner.setSelection(years.indexOf(String.valueOf(currentYear)));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
                historiqueViewModel.setYear(selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    public void onDeleteClick(Pointage pointage) {
        showDeleteConfirmationDialog(pointage);
    }

    private void showDeleteConfirmationDialog(Pointage pointage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Supprimer l'historique")
                .setMessage("Voulez-vous vraiment supprimer cet historique ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    if (pointage.getId() != null) {
                        historiqueViewModel.deletePointage(pointage.getId(), new HistoriqueViewModel.OnDeleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(requireContext(), "Pointage supprimé avec succès", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(requireContext(), "Échec de la suppression: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}