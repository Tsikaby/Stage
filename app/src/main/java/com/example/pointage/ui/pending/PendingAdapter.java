package com.example.pointage.ui.pending;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

import java.util.ArrayList;
import java.util.List;

public class PendingAdapter extends ListAdapter<PendingItem, PendingAdapter.VH> {

    public PendingAdapter() {
        super(DIFF);
    }

    public void setItems(List<PendingItem> items) {
        submitList(items == null ? new ArrayList<>() : new ArrayList<>(items));
    }

    private static final DiffUtil.ItemCallback<PendingItem> DIFF = new DiffUtil.ItemCallback<PendingItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull PendingItem oldItem, @NonNull PendingItem newItem) {
            // Same surveillant and same session row
            return oldItem.getIdSurveillant() == newItem.getIdSurveillant()
                    && safeEquals(oldItem.getSession(), newItem.getSession());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PendingItem oldItem, @NonNull PendingItem newItem) {
            return safeEquals(oldItem.getNom(), newItem.getNom())
                    && safeEquals(oldItem.getNumeroSalle(), newItem.getNumeroSalle())
                    && safeEquals(oldItem.getDeadline(), newItem.getDeadline())
                    && safeEquals(oldItem.getSession(), newItem.getSession());
        }

        private boolean safeEquals(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PendingItem it = getItem(pos);
        h.tvName.setText(it.getNom());
        h.tvRoom.setText(it.getNumeroSalle());
        h.tvTime.setText(it.getDeadline());
        if (h.tvSession != null) {
            h.tvSession.setText(it.getSession());
            // Simple coloring by session
            int color = 0xFF607D8B; // default
            if ("Matin".equalsIgnoreCase(it.getSession())) color = 0xFF4CAF50; // green
            else if ("Après-midi".equalsIgnoreCase(it.getSession()) || "Apres-midi".equalsIgnoreCase(it.getSession())) color = 0xFFFF9800; // orange
            h.tvSession.setBackgroundColor(color);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvRoom, tvTime, tvSession;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSession = itemView.findViewById(R.id.tvSession);
        }
    }
}
