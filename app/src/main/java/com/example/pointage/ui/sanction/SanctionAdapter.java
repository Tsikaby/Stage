package com.example.pointage.ui.sanction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;

import java.text.DateFormatSymbols;
import java.util.List;
import java.util.Locale;

public class SanctionAdapter extends RecyclerView.Adapter<SanctionAdapter.SanctionViewHolder> {

    private List<SurveillantSanction> sanctionList;

    public SanctionAdapter(List<SurveillantSanction> sanctionList) {
        this.sanctionList = sanctionList;
    }

    public void setSanctionList(List<SurveillantSanction> newList) {
        this.sanctionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SanctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sanction, parent, false);
        return new SanctionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SanctionViewHolder holder, int position) {
        SurveillantSanction sanction = sanctionList.get(position);
        holder.bind(sanction);
    }

    @Override
    public int getItemCount() {
        return sanctionList != null ? sanctionList.size() : 0;
    }

    static class SanctionViewHolder extends RecyclerView.ViewHolder {
        TextView nomSurveillantTextView, retardsTextView, absencesTextView, moisSanctionTextView;

        public SanctionViewHolder(@NonNull View itemView) {
            super(itemView);
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_sanction);
            retardsTextView = itemView.findViewById(R.id.retards_sanction);
            absencesTextView = itemView.findViewById(R.id.absences_sanction);
            moisSanctionTextView = itemView.findViewById(R.id.mois_sanction);
        }

        public void bind(SurveillantSanction sanction) {
            // Get month name from its number (0-indexed)
            String monthName = new DateFormatSymbols(Locale.getDefault()).getMonths()[sanction.getMois()];

            nomSurveillantTextView.setText("Surveillant: " + sanction.getNom_surveillant());
            moisSanctionTextView.setText("Mois: " + monthName);
            retardsTextView.setText("Nombre de retards: " + sanction.getNombre_retards());
            absencesTextView.setText("Nombre d'absences: " + sanction.getNombre_absences());
        }
    }
}