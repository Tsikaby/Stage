package com.example.pointage.ui.usermanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

public class UserManagementFragment extends Fragment {

    private View rootView;
    private UserManagementViewModel userManagementViewModel;
    private PendingUserAdapter adapter;
    private RecyclerView recyclerViewPendingUsers;
    private TextView textViewNoPendingUsers;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        userManagementViewModel = new ViewModelProvider(this).get(UserManagementViewModel.class);

        rootView = inflater.inflate(R.layout.fragment_user_management, container, false);

        recyclerViewPendingUsers = rootView.findViewById(R.id.recyclerViewPendingUsers);
        textViewNoPendingUsers = rootView.findViewById(R.id.textViewNoPendingUsers);

        setupRecyclerView();
        observeViewModel();

        return rootView;
    }

    private void setupRecyclerView() {
        adapter = new PendingUserAdapter();
        recyclerViewPendingUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPendingUsers.setAdapter(adapter);

        adapter.setOnUserActionListener(new PendingUserAdapter.OnUserActionListener() {
            @Override
            public void onApproveUser(String username) {
                userManagementViewModel.approveUser(username, new UserManagementViewModel.OnUserActionListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        userManagementViewModel.loadPendingUsers();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onRejectUser(String username) {
                userManagementViewModel.rejectUser(username, new UserManagementViewModel.OnUserActionListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        userManagementViewModel.loadPendingUsers();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void observeViewModel() {
        userManagementViewModel.getPendingUsers().observe(getViewLifecycleOwner(), pendingUsers -> {
            adapter.setPendingUsers(pendingUsers);
            if (pendingUsers.isEmpty()) {
                textViewNoPendingUsers.setVisibility(View.VISIBLE);
                recyclerViewPendingUsers.setVisibility(View.GONE);
            } else {
                textViewNoPendingUsers.setVisibility(View.GONE);
                recyclerViewPendingUsers.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        userManagementViewModel.loadPendingUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }
}
