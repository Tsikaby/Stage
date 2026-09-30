package com.example.pointage.ui.home;

import android.graphics.Color;
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

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> groupedNotifications = new ArrayList<>();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE);
    private static final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
    private final SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            DateHeaderViewHolder headerHolder = (DateHeaderViewHolder) holder;
            Date date = (Date) groupedNotifications.get(position);
            headerHolder.bind(date);
        } else if (holder instanceof NotificationViewHolder) {
            NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;
            Notification notification = (Notification) groupedNotifications.get(position);
            notificationHolder.bind(notification);
        }
    }

    @Override
    public int getItemCount() {
        return groupedNotifications.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = groupedNotifications.get(position);
        return item instanceof Date ? TYPE_HEADER : TYPE_ITEM;
    }

    public void setNotifications(List<Notification> notifications) {
        groupNotificationsByDate(notifications);
        notifyDataSetChanged();
    }

    private void groupNotificationsByDate(List<Notification> notifications) {
        groupedNotifications.clear();

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        // Grouper par date
        Map<String, List<Notification>> groupedByDate = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dayFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));

        for (Notification notification : notifications) {
            if (notification.getDateHeure() != null) {
                String dateKey = dayFormat.format(notification.getDateHeure());
                if (!groupedByDate.containsKey(dateKey)) {
                    groupedByDate.put(dateKey, new ArrayList<>());
                }
                groupedByDate.get(dateKey).add(notification);
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
            groupedNotifications.add(date);
            List<Notification> dayNotifications = groupedByDate.get(dayFormat.format(date));
            if (dayNotifications != null) {
                // Trier les notifications de la journée par heure décroissante
                dayNotifications.sort((n1, n2) -> n2.getDateHeure().compareTo(n1.getDateHeure()));
                groupedNotifications.addAll(dayNotifications);
            }
        }
    }

    public void addNotification(Notification notification) {
        // Logique pour ajouter une nouvelle notification au groupe approprié
        // Cette méthode deviendrait plus complexe avec le regroupement
        // Pour l'instant, on recharge simplement toutes les notifications
        // Une implémentation plus optimisée serait recommandée pour la production
    }

    public void clearNotifications() {
        groupedNotifications.clear();
        notifyDataSetChanged();
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

    // ViewHolder pour les notifications
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView salle;
        TextView datetime;
        ImageView icon;
        View badge;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            salle = itemView.findViewById(R.id.notification_salle);
            datetime = itemView.findViewById(R.id.notification_datetime);
            icon = itemView.findViewById(R.id.notification_icon);
            badge = itemView.findViewById(R.id.notification_badge);
        }

        void bind(Notification notification) {
            // Titre avec type et nom
            String typeText;
            int iconResource;

            if (notification.isRetard()) {
                typeText = "⏰ EN RETARD";
                iconResource = R.drawable.ic_retard;
                title.setTextColor(itemView.getContext().getColor(R.color.retard_color));
            } else if (notification.isAbsence()) {
                typeText = "❌ ABSENT";
                iconResource = R.drawable.ic_absence;
                title.setTextColor(itemView.getContext().getColor(R.color.absence_color));
            } else {
                typeText = "✅ A L'HEURE";
                iconResource = R.drawable.ic_presence;
                title.setTextColor(itemView.getContext().getColor(R.color.presence_color));
            }

            title.setText(typeText + " - " + notification.getNomSurveillant());
            icon.setImageResource(iconResource);

            // Salle et session pour les absences
            if (notification.isAbsence()) {
                StringBuilder salleInfo = new StringBuilder();
                if (notification.getNumeroSalle() != null && !notification.getNumeroSalle().isEmpty()) {
                    salleInfo.append("Salle: ").append(notification.getNumeroSalle());
                }
                if (notification.getSession() != null && !notification.getSession().isEmpty()) {
                    if (salleInfo.length() > 0) salleInfo.append(" - ");
                    salleInfo.append("Session: ").append(notification.getSession());
                }
                if (salleInfo.length() > 0) {
                    salle.setText(salleInfo.toString());
                    salle.setVisibility(View.VISIBLE);
                } else {
                    salle.setVisibility(View.GONE);
                }
            } else {
                // Pour les retards et présences, afficher seulement la salle
                if (notification.getNumeroSalle() != null && !notification.getNumeroSalle().isEmpty()) {
                    salle.setText("Salle: " + notification.getNumeroSalle());
                    salle.setVisibility(View.VISIBLE);
                } else {
                    salle.setVisibility(View.GONE);
                }
            }

            // Date : pour les absences afficher seulement la date, pour les autres afficher l'heure seulement
            if (notification.getDateHeure() != null) {
                if (notification.isAbsence()) {
                    dateOnlyFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));
                    datetime.setText(dateOnlyFormat.format(notification.getDateHeure()));
                } else {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.FRANCE);
                    timeFormat.setTimeZone(TimeZone.getTimeZone("Indian/Antananarivo"));
                    datetime.setText(timeFormat.format(notification.getDateHeure()));
                }
            }

            // Badge pour les notifications non lues
            badge.setVisibility(notification.isLu() ? View.GONE : View.VISIBLE);

            // Fond légèrement différent pour les notifications non lues
            if (!notification.isLu()) {
                itemView.setAlpha(1.0f);
            } else {
                itemView.setAlpha(0.75f);
            }
        }
    }
}