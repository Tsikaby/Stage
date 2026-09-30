package com.example.pointage.ui.usermanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

import java.util.ArrayList;
import java.util.List;

public class PendingUserAdapter extends RecyclerView.Adapter<PendingUserAdapter.PendingUserViewHolder> {

    public interface OnUserActionListener {
        void onApproveUser(String username);
        void onRejectUser(String username);
    }

    private List<UserManagementViewModel.PendingUser> pendingUsers;
    private OnUserActionListener listener;

    public PendingUserAdapter() {
        this.pendingUsers = new ArrayList<>();
    }

    public void setPendingUsers(List<UserManagementViewModel.PendingUser> pendingUsers) {
        this.pendingUsers = pendingUsers;
        notifyDataSetChanged();
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_user, parent, false);
        return new PendingUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingUserViewHolder holder, int position) {
        UserManagementViewModel.PendingUser user = pendingUsers.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return pendingUsers.size();
    }

    static class PendingUserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, roleTextView;
        Button approveButton, rejectButton;

        public PendingUserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.textViewUsername);
            roleTextView = itemView.findViewById(R.id.textViewRole);
            approveButton = itemView.findViewById(R.id.buttonApprove);
            rejectButton = itemView.findViewById(R.id.buttonReject);
        }

        public void bind(UserManagementViewModel.PendingUser user, OnUserActionListener listener) {
            usernameTextView.setText("Utilisateur: " + user.getUsername());
            roleTextView.setText("Rôle: " + user.getRole());

            // S'assurer que les boutons sont toujours visibles
            approveButton.setVisibility(View.VISIBLE);
            rejectButton.setVisibility(View.VISIBLE);

            approveButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApproveUser(user.getUsername());
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectUser(user.getUsername());
                }
            });
        }
    }
}









