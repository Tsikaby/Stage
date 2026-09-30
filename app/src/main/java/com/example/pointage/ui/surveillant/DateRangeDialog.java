package com.example.pointage.ui.surveillant;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.pointage.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DateRangeDialog extends DialogFragment {
    
    public interface DateRangeListener {
        void onDateRangeSelected(int year, int month);
    }
    
    private DateRangeListener listener;
    
    public static DateRangeDialog newInstance(DateRangeListener listener) {
        DateRangeDialog dialog = new DateRangeDialog();
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_date_range, null);
        
        Spinner yearSpinner = view.findViewById(R.id.spinner_year);
        Spinner monthSpinner = view.findViewById(R.id.spinner_month);
        
        // Configurer le spinner des années (2020 à année en cours)
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 2020; i <= currentYear; i++) {
            years.add(String.valueOf(i));
        }
        
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        
        // Configurer le spinner des mois
        String[] months = getResources().getStringArray(R.array.months_array);
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.months_array,
                android.R.layout.simple_spinner_item
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        
        // Définir les valeurs par défaut (mois et année actuels)
        Calendar calendar = Calendar.getInstance();
        yearSpinner.setSelection(years.indexOf(String.valueOf(calendar.get(Calendar.YEAR))));
        monthSpinner.setSelection(calendar.get(Calendar.MONTH));
        
        builder.setView(view)
                .setTitle("Sélectionner la période")
                .setPositiveButton("Générer le rapport", (dialog, which) -> {
                    int selectedYear = Integer.parseInt(years.get(yearSpinner.getSelectedItemPosition()));
                    int selectedMonth = monthSpinner.getSelectedItemPosition(); // 0-11
                    if (listener != null) {
                        listener.onDateRangeSelected(selectedYear, selectedMonth);
                    }
                })
                .setNegativeButton("Annuler", (dialog, which) -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });
                
        return builder.create();
    }
}
