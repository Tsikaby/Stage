package com.example.pointage.ui.historique;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoriqueAdapter extends RecyclerView.Adapter<HistoriqueAdapter.HistoriqueViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Pointage pointage);
    }

    private static List<Pointage> historiqueList;
    private OnDeleteClickListener listener;

    public HistoriqueAdapter(List<Pointage> historiqueList, OnDeleteClickListener listener) {
        this.historiqueList = historiqueList;
        this.listener = listener;
    }

    public void setHistoriqueList(List<Pointage> newList) {
        this.historiqueList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoriqueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historique, parent, false);
        return new HistoriqueViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoriqueViewHolder holder, int position) {
        holder.bind(historiqueList.get(position));
    }

    @Override
    public int getItemCount() {
        return historiqueList.size();
    }

    static class HistoriqueViewHolder extends RecyclerView.ViewHolder {
        private final TextView nomSurveillantTextView;
        private final TextView heureTextView;
        private final TextView retardTextView;
        private final ImageView deleteIcon;
        private OnDeleteClickListener listener;

        public HistoriqueViewHolder(@NonNull View itemView, OnDeleteClickListener listener) {
            super(itemView);
            this.listener = listener;
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_historique);
            heureTextView = itemView.findViewById(R.id.heure_historique);
            retardTextView = itemView.findViewById(R.id.retard_historique);
            deleteIcon = itemView.findViewById(R.id.delete_icon);

            deleteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (HistoriqueViewHolder.this.listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            HistoriqueViewHolder.this.listener.onDeleteClick(historiqueList.get(position));
                        }
                    }
                }
            });
        }

        public void bind(Pointage pointage) {
            nomSurveillantTextView.setText("Surveillant: " + (pointage.getNom_surveillant() != null ? pointage.getNom_surveillant() : "Inconnu"));

            Timestamp timestamp = pointage.getHeure_pointage();
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String formattedTime = sdf.format(new Date(timestamp.getSeconds() * 1000));
                heureTextView.setText("Heure de scan: " + formattedTime);
            } else {
                heureTextView.setText("Heure de scan: N/A");
            }

            // Ligne modifiée pour afficher "Oui" ou "Non"
            String retardText = pointage.isRetard() ? "Oui" : "Non";
            retardTextView.setText("Retard: " + retardText);
        }
    }
}