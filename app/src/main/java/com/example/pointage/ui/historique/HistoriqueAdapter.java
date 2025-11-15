package com.example.pointage.ui.historique;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HistoriqueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Pointage pointage);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> groupedHistorique = new ArrayList<>();
    private OnDeleteClickListener listener;

    public HistoriqueAdapter(List<Pointage> historiqueList, OnDeleteClickListener listener) {
        this.listener = listener;
        setHistoriqueList(historiqueList);
    }

    public void setHistoriqueList(List<Pointage> newList) {
        Log.d("HistoriqueAdapter", "setHistoriqueList called with " + (newList != null ? newList.size() : "null") + " items");
        groupHistoriqueByDate(newList);
        notifyDataSetChanged();
    }

    private void groupHistoriqueByDate(List<Pointage> historiqueList) {
        groupedHistorique.clear();

        if (historiqueList == null || historiqueList.isEmpty()) {
            return;
        }

        // Grouper par date
        Map<String, List<Pointage>> groupedByDate = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dayFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));

        for (Pointage pointage : historiqueList) {
            if (pointage.getHeure_pointage() != null) {
                String dateKey = dayFormat.format(pointage.getHeure_pointage());
                if (!groupedByDate.containsKey(dateKey)) {
                    groupedByDate.put(dateKey, new ArrayList<>());
                }
                groupedByDate.get(dateKey).add(pointage);
            }
        }

        // Trier les dates par ordre décroissant
        List<Date> sortedDates = new ArrayList<>();
        for (String dateKey : groupedByDate.keySet()) {
            try {
                Date date = dayFormat.parse(dateKey);
                sortedDates.add(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sortedDates.sort((d1, d2) -> d2.compareTo(d1));

        // Construire la liste groupée
        for (Date date : sortedDates) {
            groupedHistorique.add(date);
            List<Pointage> dayPointages = groupedByDate.get(dayFormat.format(date));
            if (dayPointages != null) {
                // Trier les pointages de la journée par heure décroissante
                dayPointages.sort((p1, p2) -> p2.getHeure_pointage().compareTo(p1.getHeure_pointage()));
                groupedHistorique.addAll(dayPointages);
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_historique, parent, false);
            return new HistoriqueViewHolder(view, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            DateHeaderViewHolder headerHolder = (DateHeaderViewHolder) holder;
            Date date = (Date) groupedHistorique.get(position);
            headerHolder.bind(date);
        } else if (holder instanceof HistoriqueViewHolder) {
            HistoriqueViewHolder historiqueHolder = (HistoriqueViewHolder) holder;
            Pointage pointage = (Pointage) groupedHistorique.get(position);
            historiqueHolder.bind(pointage);
        }
    }

    @Override
    public int getItemCount() {
        return groupedHistorique.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = groupedHistorique.get(position);
        return item instanceof Date ? TYPE_HEADER : TYPE_ITEM;
    }

    // ViewHolder pour l'en-tête de date
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_header_text);
        }

        void bind(Date date) {
            SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
            headerFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));
            dateTextView.setText(headerFormat.format(date));
        }
    }

    // ViewHolder pour les pointages
    class HistoriqueViewHolder extends RecyclerView.ViewHolder {
        private final TextView nomSurveillantTextView;
        private final TextView heureTextView;
        private final TextView retardTextView;
        private final TextView salleTextView;
        private final TextView statusIcon;
        private final ImageView deleteIcon;
        private final View retardBadge;
        private final View statusBadge;
        private OnDeleteClickListener listener;

        public HistoriqueViewHolder(@NonNull View itemView, OnDeleteClickListener listener) {
            super(itemView);
            this.listener = listener;
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_historique);
            heureTextView = itemView.findViewById(R.id.heure_historique);
            retardTextView = itemView.findViewById(R.id.retard_historique);
            salleTextView = itemView.findViewById(R.id.salle_historique);
            statusIcon = itemView.findViewById(R.id.status_icon);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
            retardBadge = itemView.findViewById(R.id.retard_badge);
            statusBadge = itemView.findViewById(R.id.status_badge);

            deleteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (HistoriqueViewHolder.this.listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Object item = groupedHistorique.get(position);
                            if (item instanceof Pointage) {
                                HistoriqueViewHolder.this.listener.onDeleteClick((Pointage) item);
                            }
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

            // Afficher l'heure de scan seulement (la date est dans l'en-tête)
            Date date = pointage.getHeure_pointage();
            if (date != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault());
                timeFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));
                String formattedTime = timeFormat.format(date);
                heureTextView.setText("Heure de scan : " + formattedTime);
            } else {
                heureTextView.setText("Heure de scan : N/A");
            }

            // Afficher retard : Oui ou Non
            retardTextView.setText("Retard : " + (pointage.isRetard() ? "Oui" : "Non"));

            // Montrer le badge de retard si nécessaire
            retardBadge.setVisibility(pointage.isRetard() ? View.VISIBLE : View.GONE);

            // Changer l'icône de statut selon le retard
            statusIcon.setText(pointage.isRetard() ? "⏰" : "✅");
            statusBadge.setBackgroundTintList(pointage.isRetard() ?
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE")) :
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));

            // Afficher le numéro de salle
            String numeroSalle = pointage.getNumero_salle();
            salleTextView.setText("Salle : " +
                    (numeroSalle != null && !numeroSalle.isEmpty() ? numeroSalle : "Non spécifiée"));
        }
    }
}