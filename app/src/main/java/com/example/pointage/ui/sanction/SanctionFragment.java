package com.example.pointage.ui.sanction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pointage.databinding.FragmentSanctionBinding;

public class SanctionFragment extends Fragment {

    private FragmentSanctionBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SanctionViewModel sanctionViewModel =
                new ViewModelProvider(this).get(SanctionViewModel.class);

        binding = FragmentSanctionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSanction;
        SanctionViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}