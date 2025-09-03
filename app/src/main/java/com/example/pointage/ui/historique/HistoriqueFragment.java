package com.example.pointage.ui.historique;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pointage.databinding.FragmentHistoriqueBinding;

public class HistoriqueFragment extends Fragment {

    private FragmentHistoriqueBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HistoriqueViewModel historiqueViewModel =
                new ViewModelProvider(this).get(HistoriqueViewModel.class);

        binding = FragmentHistoriqueBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHistorique;
        historiqueViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}