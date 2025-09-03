package com.example.pointage.ui.surveillant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pointage.databinding.FragmentSurveillantBinding;

public class SurveillantFragment extends Fragment {

    private FragmentSurveillantBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SurveillantViewModel surveillantViewModel =
                new ViewModelProvider(this).get(SurveillantViewModel.class);

        binding = FragmentSurveillantBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSurveillant;
        SurveillantViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}