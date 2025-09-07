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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.DateFormatSymbols;

public class SanctionFragment extends Fragment {

    private FragmentSanctionBinding binding;
    private SanctionViewModel sanctionViewModel;
    private SanctionAdapter sanctionAdapter;
    private Spinner spinnerMonth, spinnerYear;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sanctionViewModel = new ViewModelProvider(this).get(SanctionViewModel.class);
        binding = FragmentSanctionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewSanction;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sanctionAdapter = new SanctionAdapter(new ArrayList<>());
        recyclerView.setAdapter(sanctionAdapter);

        spinnerMonth = binding.spinnerMonth;
        spinnerYear = binding.spinnerYear;

        // Populate the month spinner
        String[] months = new DateFormatSymbols().getMonths();
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // Populate the year spinner
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear; i++) { // Past 5 years and current year
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Set listeners for the spinners
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedMonth = spinnerMonth.getSelectedItemPosition();
                int selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());
                sanctionViewModel.loadSanctions(selectedMonth, selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        };

        spinnerMonth.setOnItemSelectedListener(spinnerListener);
        spinnerYear.setOnItemSelectedListener(spinnerListener);

        // Select the current month and year by default
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));
        spinnerYear.setSelection(years.indexOf(String.valueOf(currentYear)));

        sanctionViewModel.getSanctions().observe(getViewLifecycleOwner(), sanctionList -> {
            if (sanctionList != null) {
                sanctionAdapter.setSanctionList(sanctionList);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}