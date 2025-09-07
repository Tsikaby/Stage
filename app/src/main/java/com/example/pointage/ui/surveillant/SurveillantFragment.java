package com.example.pointage.ui.surveillant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.databinding.FragmentSurveillantBinding;

import java.util.ArrayList;

public class SurveillantFragment extends Fragment {

    private FragmentSurveillantBinding binding;
    private SurveillantViewModel surveillantViewModel;
    private SurveillantAdapter surveillantAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        surveillantViewModel = new ViewModelProvider(this).get(SurveillantViewModel.class);
        binding = FragmentSurveillantBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewSurveillants;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        surveillantAdapter = new SurveillantAdapter(new ArrayList<>());
        recyclerView.setAdapter(surveillantAdapter);

        surveillantViewModel.getSurveillants().observe(getViewLifecycleOwner(), surveillants -> {
            if (surveillants != null) {
                surveillantAdapter.setSurveillantList(surveillants);
            }
        });

        // Set up the SearchView to filter the list
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}