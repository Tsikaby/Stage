package com.example.pointage.ui.historique;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoriqueAdapter extends RecyclerView.Adapter<HistoriqueAdapter.HistoriqueViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Pointage pointage);
    }

    private List<Pointage> historiqueList;
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

    class HistoriqueViewHolder extends RecyclerView.ViewHolder {
        private final TextView nomSurveillantTextView;
        private final TextView heureTextView;
        private final TextView retardTextView;
        private final TextView salleTextView;
        private final ImageView deleteIcon;
        private OnDeleteClickListener listener;

        public HistoriqueViewHolder(@NonNull View itemView, OnDeleteClickListener listener) {
            super(itemView);
            this.listener = listener;
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_historique);
            heureTextView = itemView.findViewById(R.id.heure_historique);
            retardTextView = itemView.findViewById(R.id.retard_historique);
            salleTextView = itemView.findViewById(R.id.salle_historique);
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
            // Afficher le nom du surveillant
            String nomSurveillant = pointage.getNom_surveillant();
            nomSurveillantTextView.setText("Surveillant : " +
                    (nomSurveillant != null && !nomSurveillant.isEmpty() ? nomSurveillant : "Inconnu"));

            // Afficher la date et l'heure de scan
            Date date = pointage.getHeure_pointage();
            if (date != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss", Locale.getDefault());
                String formattedDateTime = sdf.format(date);
                heureTextView.setText("Date et heure de scan : " + formattedDateTime);
            } else {
                heureTextView.setText("Date et heure de scan : N/A");
            }

            // Afficher retard : Oui ou Non
            retardTextView.setText("Retard : " + (pointage.isRetard() ? "Oui" : "Non"));

            // Afficher le numéro de salle
            String numeroSalle = pointage.getNumero_salle();
            salleTextView.setText("Salle : " +
                    (numeroSalle != null && !numeroSalle.isEmpty() ? numeroSalle : "Non spécifiée"));
        }
    }
}