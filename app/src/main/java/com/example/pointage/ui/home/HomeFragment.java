package com.example.pointage.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NotificationAdapter notificationAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        // Définir le contexte pour les notifications externes
        if (getContext() != null) {
            homeViewModel.setContext(getContext());
            homeViewModel.initializeSharedPreferences();
        }
        
        // Configuration de la RecyclerView pour les notifications
        RecyclerView recyclerView = binding.notificationsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationAdapter = new NotificationAdapter();
        recyclerView.setAdapter(notificationAdapter);
        
        // Observer les notifications
        homeViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                notificationAdapter.setNotifications(notifications);
                binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
                binding.emptyNotificationsLayout.setVisibility(View.GONE);
            } else {
                binding.notificationsRecyclerView.setVisibility(View.GONE);
                binding.emptyNotificationsLayout.setVisibility(View.VISIBLE);
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