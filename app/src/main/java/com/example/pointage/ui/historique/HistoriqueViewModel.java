package com.example.pointage.ui.historique;

import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ConnectClient;
import com.example.pointage.ui.historique.DateUtils;
import com.example.pointage.utils.NotificationHelper;
//import com.example.pointage.utils.EmailUtility;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.Normalizer;
import java.text.SimpleDateFormat;

public class HistoriqueViewModel extends ViewModel {

    // Nombre d'heures avant l'heure de début où le scan est autorisé
    private static final int SCAN_EARLY_HOURS = 2;
    private static final String PREFS_NAME = "sanction_prefs";
    private static final String KEY_NOTIFIED_SANCTIONS = "notified_sanctions";

    private SharedPreferences sharedPreferences;
    private final Set<String> notifiedSanctions = new HashSet<>();

    private String getSession(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour < 12 ? "Matin" : "Après-midi";
    }

    // Normaliser les noms de salles pour la comparaison (trim, uppercase, remove accents)
    private String normalizeRoomName(String roomName) {
        if (roomName == null) return "";
        String normalized = roomName.trim().toUpperCase(Locale.getDefault());
        // Supprimer les accents
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        // Remplacer les espaces multiples par un seul espace
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    private final MutableLiveData<List<Pointage>> historiqueLiveData = new MutableLiveData<>();
    private final ConnectClient connectClient = ConnectClient.getInstance();
    private final Handler absenceCheckHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> scheduledAbsenceChecks = new HashMap<>();

    private void scheduleAbsenceCheck(String date, String session, long triggerAtMillis) {
        String key = date + "_" + session;
        Runnable existing = scheduledAbsenceChecks.remove(key);
        if (existing != null) {
            absenceCheckHandler.removeCallbacks(existing);
        }

        Runnable task = () -> {
            scheduledAbsenceChecks.remove(key);
            checkSessionAbsences();
        };
        scheduledAbsenceChecks.put(key, task);
        long delay = Math.max(0, triggerAtMillis - System.currentTimeMillis());
        absenceCheckHandler.postDelayed(task, delay);
        Log.d("HistoriqueViewModel", "Absence check scheduled for " + key + " in " + delay + "ms");
    }

    private int monthFilter = Calendar.getInstance().get(Calendar.MONTH);
    private int yearFilter = Calendar.getInstance().get(Calendar.YEAR);

    public HistoriqueViewModel() throws UnknownHostException {
        loadHistorique();
    }

    /**
     * Initialiser SharedPreferences pour tracker les notifications affichées
     */
    public void initializeSharedPreferences(Context context) {
        if (sharedPreferences == null && context != null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            loadNotifiedSanctionsFromPrefs();
        }
    }

    /**
     * Charger les sanctions notifiées depuis SharedPreferences
     */
    private void loadNotifiedSanctionsFromPrefs() {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(KEY_NOTIFIED_SANCTIONS, "");
            if (!json.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    String[] notified = gson.fromJson(json, String[].class);
                    if (notified != null) {
                        for (String key : notified) {
                            notifiedSanctions.add(key);
                        }
                        Log.d("HistoriqueViewModel", "Loaded " + notifiedSanctions.size() + " previously notified sanctions");
                    }
                } catch (Exception e) {
                    Log.e("HistoriqueViewModel", "Erreur lors du chargement des sanctions notifiées", e);
                }
            }
        }
    }

    /**
     * Vérifier si une sanction a déjà été notifiée
     */
    private boolean isAlreadyNotified(Long surveillantId, String type, String date, String session) {
        String key = surveillantId + "_" + type + "_" + date + "_" + session;
        return notifiedSanctions.contains(key);
    }

    /**
     * Marquer une sanction comme notifiée et sauvegarder
     */
    private void markAsNotified(Long surveillantId, String type, String date, String session) {
        String key = surveillantId + "_" + type + "_" + date + "_" + session;
        if (!notifiedSanctions.contains(key)) {
            notifiedSanctions.add(key);
            saveNotifiedSanctionsToPrefs();
        }
    }

    /**
     * Sauvegarder les sanctions notifiées dans SharedPreferences
     */
    private void saveNotifiedSanctionsToPrefs() {
        if (sharedPreferences != null) {
            try {
                Gson gson = new Gson();
                String json = gson.toJson(notifiedSanctions.toArray());
                sharedPreferences.edit().putString(KEY_NOTIFIED_SANCTIONS, json).apply();
                Log.d("HistoriqueViewModel", "Notified sanctions saved: " + notifiedSanctions.size());
            } catch (Exception e) {
                Log.e("HistoriqueViewModel", "Erreur lors de la sauvegarde des sanctions notifiées", e);
            }
        }
    }
    public void setMonth(int month) {
        this.monthFilter = month;
        loadHistorique();
    }

    public void setYear(int year) {
        this.yearFilter = year;
        loadHistorique();
    }

    public void loadHistorique() {
        // Filtrer côté serveur par mois/année sélectionnés
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.YEAR, yearFilter);
        startCal.set(Calendar.MONTH, monthFilter); // 0-based
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        String start = DateUtils.formatForSupabase(startCal.getTime());

        Calendar nextCal = (Calendar) startCal.clone();
        nextCal.add(Calendar.MONTH, 1);
        String nextStart = DateUtils.formatForSupabase(nextCal.getTime());

        Log.d("HistoriqueViewModel", "Date range: start=" + start + ", nextStart=" + nextStart);
        String filter = "heure_pointage=gte." + start
                + "&heure_pointage=lt." + nextStart
                + "&order=heure_pointage.desc";
        Log.d("HistoriqueViewModel", "Filter string: " + filter);

        connectClient.select("pointage", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Log.d("HistoriqueViewModel", "loadHistorique: received " + result.size() + " pointages from Supabase");
                List<Pointage> allPointages = new ArrayList<>();
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject doc = result.get(i).getAsJsonObject();
                        Pointage p = new Pointage();
                        p.setId(String.valueOf(doc.get("id_pointage").getAsLong()));
                        p.setId_pointage(doc.get("id_pointage").getAsLong());

                        // Conversion du timestamp Supabase en Date
                        String timestampStr = doc.get("heure_pointage").getAsString();
                        Date date = DateUtils.parseSupabaseTimestamp(timestampStr);
                        p.setHeure_pointage(date);

                        // Safety: filtre côté client si jamais le serveur renvoie hors intervalle
                        Calendar c = Calendar.getInstance();
                        c.setTime(date);
                        if (c.get(Calendar.YEAR) != yearFilter || c.get(Calendar.MONTH) != monthFilter) {
                            continue; // skip
                        }

                        p.setId_surveillant(doc.get("id_surveillant").getAsLong());
                        p.setRetard(doc.get("retard").getAsBoolean());

                        // Numero salle depuis pointage
                        if (doc.has("numero_salle") && !doc.get("numero_salle").isJsonNull()) {
                            p.setNumero_salle(doc.get("numero_salle").getAsString());
                        } else {
                            p.setNumero_salle(null);
                        }

                        // Nom surveillant sera rempli par fetchSurveillantDataAndBind
                        p.setNom_surveillant("Chargement...");
                        allPointages.add(p);
                    }
                    Log.d("HistoriqueViewModel", "loadHistorique: after filtering, " + allPointages.size() + " pointages remain");
                    fetchSurveillantDataAndBind(allPointages);
                } catch (Exception e) {
                    Log.e("HistoriqueViewModel", "loadHistorique: error parsing pointages", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("HistoriqueViewModel", "loadHistorique: error loading pointages", error);
                error.printStackTrace();
            }
        });
    }

    public void performScan(String numeroSalleSurveillant, Long idSurveillant, String nomSurveillant, final OnScanResultListener listener) {
        Date now = new Date();
        String session = getSession(now);
        String currentDate = DateUtils.getCurrentDateString();

        Log.d("HistoriqueViewModel", "performScan: numeroSalleSurveillant=" + numeroSalleSurveillant + ", idSurveillant=" + idSurveillant + ", nomSurveillant=" + nomSurveillant + ", currentDate=" + currentDate);

        // Vérifier d'abord si le surveillant est planifié dans cette salle à cette heure
        // via la table planning_surveillance (jointure avec examen pour la date)
        // On filtre directement par id_surveillant
        String planningFilter = "id_surveillant=eq." + idSurveillant;

        connectClient.select("planning_surveillance", "*", planningFilter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                Log.d("HistoriqueViewModel", "planningResult.size()=" + planningResult.size());
                if (planningResult.size() == 0) {
                    if (listener != null) listener.onScanFailure("Ce surveillant n'est pas planifié dans cette salle aujourd'hui.");
                    return;
                }

                // Vérifier si le surveillant est planifié dans la salle scannée
                // La table planning_surveillance a une ligne par surveillant/salle
                boolean anyMatch = false;
                final String[] matchedSalleHolder = {null};
                Long matchedIdExamen = null;

                for (int i = 0; i < planningResult.size(); i++) {
                    JsonObject planning = planningResult.get(i).getAsJsonObject();

                    // Vérifier si la salle correspond
                    String numeroSallePlanning = planning.get("numero_salle").getAsString();
                    Log.d("HistoriqueViewModel", "Planning " + i + ": numeroSallePlanning=" + numeroSallePlanning + ", id_surveillant=" + planning.get("id_surveillant").getAsLong());

                    // Normaliser les noms de salles pour la comparaison (trim, uppercase, remove accents)
                    String normalizedPlanningSalle = normalizeRoomName(numeroSallePlanning);
                    String normalizedSurveillantSalle = normalizeRoomName(numeroSalleSurveillant);

                    Log.d("HistoriqueViewModel", "Normalized salle comparison: planning='" + normalizedPlanningSalle + "' vs surveillant='" + normalizedSurveillantSalle + "'");

                    if (normalizedPlanningSalle.equals(normalizedSurveillantSalle)) {
                        anyMatch = true;
                        matchedSalleHolder[0] = numeroSallePlanning.trim();
                        matchedIdExamen = planning.get("id_examen").getAsLong();
                        Log.d("HistoriqueViewModel", "MATCH FOUND! Surveillant is scheduled in salle " + matchedSalleHolder[0]);
                        break;
                    }
                }

                Log.d("HistoriqueViewModel", "Final anyMatch value: " + anyMatch);
                if (!anyMatch) {
                    if (listener != null) listener.onScanFailure("Ce surveillant n'est pas planifié dans cette salle aujourd'hui.");
                    return;
                }

                final Long finalMatchedIdExamen = matchedIdExamen;

                // Filtrer par heure: vérifier si maintenant est entre heure_debut et heure_fin
                // On vérifie uniquement pour le planning qui correspond à la salle ET à la session
                boolean isScheduledNow = false;
                for (int i = 0; i < planningResult.size(); i++) {
                    JsonObject planning = planningResult.get(i).getAsJsonObject();

                    // Vérifier si ce planning correspond à la salle matchée
                    String numeroSallePlanning = planning.get("numero_salle").getAsString();
                    if (!numeroSallePlanning.trim().equals(matchedSalleHolder[0])) {
                        continue;
                    }

                    // Vérifier l'heure pour ce planning spécifique
                    try {
                        // Les colonnes heure_debut et heure_fin sont de type timestamp
                        String heureDebutStr = planning.get("heure_debut").getAsString();
                        Log.d("HistoriqueViewModel", "Checking time for planning: heure_debut=" + heureDebutStr);

                        // Parser le timestamp complet
                        Date heureDebut = DateUtils.parseSupabaseTimestamp(heureDebutStr);

                        // Vérifier si l'examen appartient à la même session que le scan actuel
                        String examenSession = getSession(heureDebut);
                        if (!examenSession.equals(session)) {
                            Log.d("HistoriqueViewModel", "Exam session (" + examenSession + ") does not match current session (" + session + "), skipping");
                            continue;
                        }

                        // Permettre le pointage SCAN_EARLY_HOURS avant le début de l'examen
                        long startMs = heureDebut.getTime() - (SCAN_EARLY_HOURS * 60 * 60 * 1000); // -SCAN_EARLY_HOURS heures en millisecondes

                        Date heureFin = null;
                        if (planning.has("heure_fin") && !planning.get("heure_fin").isJsonNull()) {
                            String heureFinStr = planning.get("heure_fin").getAsString();
                            heureFin = DateUtils.parseSupabaseTimestamp(heureFinStr);
                            Log.d("HistoriqueViewModel", "heure_fin=" + heureFinStr);
                        }
                        if (heureFin == null) heureFin = heureDebut;
                        long endMs = heureFin.getTime();

                        long nowMs = now.getTime();

                        // Créer des dates pour l'affichage dans les logs
                        Date allowedStartTime = new Date(startMs);

                        Log.d("HistoriqueViewModel", "Time check: nowMs=" + nowMs + ", startMs=" + startMs + ", endMs=" + endMs);
                        Log.d("HistoriqueViewModel", "Current time: " + now.toString());
                        Log.d("HistoriqueViewModel", "Allowed start time (2h before exam): " + allowedStartTime.toString());
                        Log.d("HistoriqueViewModel", "Exam start time: " + heureDebut.toString());
                        Log.d("HistoriqueViewModel", "End time: " + heureFin.toString());
                        Log.d("HistoriqueViewModel", "Session: " + session);

                        if (nowMs >= startMs && nowMs <= endMs) {
                            isScheduledNow = true;
                            Log.d("HistoriqueViewModel", "Time match found! isScheduledNow=true");
                            long scheduleTrigger = endMs + (5 * 60 * 1000L); // buffer de 5 minutes après la fin
                            scheduleAbsenceCheck(currentDate, session, scheduleTrigger);
                            break;
                        } else {
                            Log.d("HistoriqueViewModel", "Time does not match. nowMs >= startMs: " + (nowMs >= startMs) + ", nowMs <= endMs: " + (nowMs <= endMs));
                        }
                    } catch (Exception e) {
                        Log.e("HistoriqueViewModel", "Error parsing time for planning entry", e);
                        // Ignore parsing errors for this planning entry
                    }
                }
                Log.d("HistoriqueViewModel", "Final isScheduledNow value: " + isScheduledNow);

                if (!isScheduledNow) {
                    if (listener != null) listener.onScanFailure("Ce surveillant n'est pas planifié dans cette salle pour la session " + session + ".");
                    return;
                }

                // Calculer les bornes de session (premier début, dernière fin) pour la salle et la session du jour
                long earliestStartMs = Long.MAX_VALUE;
                long latestEndMs = Long.MIN_VALUE;
                try {
                    for (int i = 0; i < planningResult.size(); i++) {
                        JsonObject planning = planningResult.get(i).getAsJsonObject();
                        String numeroSallePlanning = planning.get("numero_salle").getAsString();
                        if (!numeroSallePlanning.trim().equals(matchedSalleHolder[0])) continue;
                        // Heure de début / fin
                        String heureDebutStr = planning.get("heure_debut").getAsString();
                        Date heureDebut = DateUtils.parseSupabaseTimestamp(heureDebutStr);
                        // Filtrer par session
                        if (!getSession(heureDebut).equals(session)) continue;
                        // S'assurer que c'est le même jour
                        Calendar debCal = Calendar.getInstance();
                        debCal.setTime(heureDebut);
                        String debDay = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                debCal.get(Calendar.YEAR), debCal.get(Calendar.MONTH)+1, debCal.get(Calendar.DAY_OF_MONTH));
                        if (!debDay.equals(currentDate)) continue;

                        long startMs = heureDebut.getTime();
                        Date heureFin = null;
                        if (planning.has("heure_fin") && !planning.get("heure_fin").isJsonNull()) {
                            String heureFinStr = planning.get("heure_fin").getAsString();
                            try { heureFin = DateUtils.parseSupabaseTimestamp(heureFinStr); } catch (Exception ignored) {}
                        }
                        if (heureFin == null) heureFin = heureDebut;
                        long endMs = heureFin.getTime();

                        if (startMs < earliestStartMs) earliestStartMs = startMs;
                        if (endMs > latestEndMs) latestEndMs = endMs;
                    }
                } catch (Exception e) {
                    Log.e("HistoriqueViewModel", "Erreur lors du calcul des bornes de session", e);
                }

                if (earliestStartMs == Long.MAX_VALUE || latestEndMs == Long.MIN_VALUE) {
                    if (listener != null) listener.onScanFailure("Aucun examen correspondant à cette session/salle aujourd'hui.");
                    return;
                }

                // Vérifier d'abord si le surveillant a déjà pointé pour cette session aujourd'hui
                String startDate = currentDate + "T00:00:00";
                String endDate = currentDate + "T23:59:59";
                String pointageFilter = "id_surveillant=eq." + idSurveillant + "&heure_pointage=gte." + startDate + "&heure_pointage=lte." + endDate;

                long finalEarliestStartMs = earliestStartMs;
                long finalLatestEndMs = latestEndMs;
                long finalLatestEndMs1 = latestEndMs;
                connectClient.select("pointage", "*", pointageFilter, new ConnectClient.ClientCallback() {
                    @Override
                    public void onSuccess(JsonArray pointageResult) {
                        // Vérifier si un pointage existe déjà pour cette session
                        boolean hasPointageInCurrentSession = false;
                        try {
                            for (int i = 0; i < pointageResult.size(); i++) {
                                JsonObject p = pointageResult.get(i).getAsJsonObject();
                                String heurePointageStr = p.get("heure_pointage").getAsString();
                                Date pointageTime = DateUtils.parseSupabaseTimestamp(heurePointageStr);
                                String pointageSession = getSession(pointageTime);

                                if (pointageSession.equals(session)) {
                                    hasPointageInCurrentSession = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.e("HistoriqueViewModel", "Error checking existing pointages", e);
                        }

                        if (hasPointageInCurrentSession) {
                            if (listener != null) {
                                listener.onScanFailure("Vous avez déjà pointé pour la session " + session + " aujourd'hui.");
                            }
                            return;
                        }

                        // Appliquer les règles de pointage basées sur les bornes de session calculées
                        Calendar nowCal = Calendar.getInstance();
                        nowCal.setTime(now);
                        long nowMs = nowCal.getTimeInMillis();

                        long allowedStartMs = finalEarliestStartMs - (SCAN_EARLY_HOURS * 60L * 60L * 1000L);
                        Log.d("HistoriqueViewModel", "Session bounds: allowedStartMs=" + new Date(allowedStartMs)
                                + ", earliestStartMs=" + new Date(finalEarliestStartMs)
                                + ", latestEndMs=" + new Date(finalLatestEndMs)
                                + ", now=" + now.toString());

                        if (nowMs < allowedStartMs) {
                            if (listener != null) listener.onScanFailure("Scan non autorisé: trop tôt (autorisé " + SCAN_EARLY_HOURS + "h avant le début).");
                            return;
                        }

                        if (nowMs > finalLatestEndMs1) {
                            // Après la dernière fin d'examen: absent (seulement si l'examen est déjà passé)
                            // Vérifier que l'examen est réellement passé (pas juste la limite de scan)
                            Date now = new Date();
                            if (now.getTime() > finalLatestEndMs1) {
                                final String absenceDate = currentDate;
                                checkAndInsertSanction(idSurveillant, "ABSENCE", currentDate, matchedSalleHolder[0], session, nomSurveillant, new ConnectClient.ClientCallback() {
                                    @Override public void onSuccess(JsonArray result) {
                                        Log.i("HistoriqueViewModel", "Absence enregistrée automatiquement: surveillant " + idSurveillant + " - " + session);
                                        // ✅ Afficher notification de l'absence (une seule fois, persistante après redémarrage)
                                        NotificationHelper.showAbsenceNotification(idSurveillant, nomSurveillant, matchedSalleHolder[0], session, absenceDate);
                                        // ✅ Envoyer email de notification d'absence
                                        //fetchSurveillantEmailAndSendNotification(idSurveillant, nomSurveillant, absenceDate, "ABSENCE");
                                    }
                                    @Override public void onError(Exception e) { e.printStackTrace(); }
                                });
                            }
                            if (listener != null) listener.onScanFailure("Pointage refusé: vous êtes considéré absent pour " + session + " (salle " + numeroSalleSurveillant + ").");
                            return;
                        }

                        boolean isRetard = nowMs >= finalEarliestStartMs && nowMs <= finalLatestEndMs;

                        String formattedDateTime = DateUtils.formatForSupabase(now);
                        JsonObject newPointageData = new JsonObject();
                        newPointageData.addProperty("heure_pointage", formattedDateTime);
                        newPointageData.addProperty("id_surveillant", idSurveillant);
                        newPointageData.addProperty("retard", isRetard);
                        newPointageData.addProperty("numero_salle", matchedSalleHolder[0]);

                        connectClient.insert("pointage", newPointageData, new ConnectClient.ClientCallback() {
                            @Override
                            public void onSuccess(JsonArray insertResult) {
                                // Enregistrer le retard si nécessaire
                                if (isRetard) {
                                    final String retardDate = currentDate;
                                    checkAndInsertSanction(idSurveillant, "RETARD", currentDate, matchedSalleHolder[0], session, nomSurveillant, new ConnectClient.ClientCallback() {
                                        @Override public void onSuccess(JsonArray result) {
                                            // ✅ Afficher notification du retard (une seule fois, persistante après redémarrage)
                                            NotificationHelper.showRetardNotification(idSurveillant, nomSurveillant, matchedSalleHolder[0], retardDate);
                                            // ✅ Envoyer email de notification de retard
                                            //fetchSurveillantEmailAndSendNotification(idSurveillant, nomSurveillant, retardDate, "RETARD");
                                        }
                                        @Override public void onError(Exception e) { e.printStackTrace(); }
                                    });
                                } else {
                                    // ✅ Afficher notification de la présence (à l'heure) (une seule fois, persistante après redémarrage)
                                    final String presenceDate = currentDate;
                                    NotificationHelper.showPresenceNotification(idSurveillant, nomSurveillant, matchedSalleHolder[0], presenceDate);
                                }
                                if (listener != null) {
                                    String message = isRetard ?
                                            "Pointage enregistré avec succès pour " + nomSurveillant + "! (En retard)" :
                                            "Pointage enregistré avec succès pour " + nomSurveillant + "! (À l'heure)";
                                    listener.onScanSuccess(message);
                                }
                                loadHistorique();
                            }

                            @Override
                            public void onError(Exception e) {
                                if (listener != null) listener.onScanFailure("Erreur d'enregistrement: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        if (listener != null) listener.onScanFailure("Échec de la vérification des pointages existants: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                if (listener != null) listener.onScanFailure("Échec de la vérification du surveillant: " + error.getMessage());
            }
        });
    }

    public interface OnScanResultListener {
        void onScanSuccess(String message);
        void onScanFailure(String errorMessage);
    }

    public void getAllHistoriqueForPdf(OnHistoriqueLoadedListener listener) {
        // Charger TOUS les pointages sans filtre de date
        String filter = "order=heure_pointage.desc";

        connectClient.select("pointage", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                List<Pointage> allPointages = new ArrayList<>();
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject doc = result.get(i).getAsJsonObject();
                        Pointage p = new Pointage();
                        p.setId(String.valueOf(doc.get("id_pointage").getAsLong()));
                        p.setId_pointage(doc.get("id_pointage").getAsLong());

                        String timestampStr = doc.get("heure_pointage").getAsString();
                        Date date = DateUtils.parseSupabaseTimestamp(timestampStr);
                        p.setHeure_pointage(date);

                        p.setId_surveillant(doc.get("id_surveillant").getAsLong());
                        p.setRetard(doc.get("retard").getAsBoolean());

                        if (doc.has("numero_salle") && !doc.get("numero_salle").isJsonNull()) {
                            p.setNumero_salle(doc.get("numero_salle").getAsString());
                        }

                        // Nom surveillant
                        if (doc.has("nom_surveillant") && !doc.get("nom_surveillant").isJsonNull()) {
                            p.setNom_surveillant(doc.get("nom_surveillant").getAsString());
                        }

                        allPointages.add(p);
                    }
                    listener.onHistoriqueLoaded(allPointages);
                } catch (Exception e) {
                    listener.onError(e);
                }
            }

            @Override
            public void onError(Exception error) {
                listener.onError(error);
            }
        });
    }

    private void checkAndInsertSanction(long idSurveillant, String typeSanction, String dateExamen, String numeroSalle, String session, String nomSurveillant, ConnectClient.ClientCallback callback) {
        // Check if sanction already exists
        String filter = "id_surveillant=eq." + idSurveillant + "&type=eq." + typeSanction + "&date_examen=eq." + dateExamen + "&numero_salle=eq." + numeroSalle + "&session=eq." + session;
        connectClient.select("sanction", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (result.size() == 0) {
                    // No existing sanction, insert
                    JsonObject sanctionData = new JsonObject();
                    sanctionData.addProperty("id_surveillant", idSurveillant);
                    sanctionData.addProperty("type", typeSanction);
                    sanctionData.addProperty("date_examen", dateExamen); // Date d'examen (pas date de création)
                    sanctionData.addProperty("numero_salle", numeroSalle);
                    sanctionData.addProperty("session", session);
                    sanctionData.addProperty("nom_surveillant", nomSurveillant);
                    // date_creation comme TIME (HH:mm:ss) pas timestamp complet
                    String currentTime = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new Date());
                    sanctionData.addProperty("date_creation", currentTime);
                    connectClient.insert("sanction", sanctionData, callback);
                } else {
                    // Already exists, do nothing
                    try {
                        if (callback != null) callback.onSuccess(result);
                    } catch (UnknownHostException e) {
                        if (callback != null) callback.onError(e);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private void fetchSurveillantDataAndBind(List<Pointage> pointageList) {
        Log.d("HistoriqueViewModel", "fetchSurveillantDataAndBind: processing " + pointageList.size() + " pointages");
        List<Long> surveillantIds = new ArrayList<>();
        for (Pointage p : pointageList) {
            if (p.getId_surveillant() != null) {
                surveillantIds.add(p.getId_surveillant());
            }
        }

        if (surveillantIds.isEmpty()) {
            Log.d("HistoriqueViewModel", "fetchSurveillantDataAndBind: no surveillant IDs, setting empty list");
            historiqueLiveData.setValue(pointageList);
            return;
        }

        Log.d("HistoriqueViewModel", "fetchSurveillantDataAndBind: fetching data for " + surveillantIds.size() + " surveillants");

        // Interroger la table surveillant
        String surveillantIdsStr = String.join(",", surveillantIds.stream().map(String::valueOf).toArray(String[]::new));
        String filter = "id_surveillant=in.(" + surveillantIdsStr + ")";

        connectClient.select("surveillant", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Log.d("HistoriqueViewModel", "fetchSurveillantDataAndBind: received " + result.size() + " surveillants");
                Map<Long, String> surveillantNameMap = new HashMap<>();

                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject doc = result.get(i).getAsJsonObject();
                        Long id = doc.get("id_surveillant").getAsLong();
                        surveillantNameMap.put(id, doc.get("nom_surveillant").getAsString());
                    }

                    // Associer les données aux pointages
                    for (Pointage p : pointageList) {
                        if (p.getId_surveillant() != null) {
                            String nom = surveillantNameMap.get(p.getId_surveillant());
                            p.setNom_surveillant(nom != null ? nom : "Surveillant inconnu");
                        } else {
                            p.setNom_surveillant("Surveillant inconnu");
                        }
                    }

                    Log.d("HistoriqueViewModel", "fetchSurveillantDataAndBind: setting LiveData with " + pointageList.size() + " pointages");
                    historiqueLiveData.setValue(pointageList);
                } catch (Exception e) {
                    Log.e("HistoriqueViewModel", "fetchSurveillantDataAndBind: error processing surveillants", e);
                    e.printStackTrace();
                    historiqueLiveData.setValue(pointageList);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("HistoriqueViewModel", "fetchSurveillantDataAndBind: error fetching surveillants", error);
                historiqueLiveData.setValue(pointageList);
            }
        });
    }

    private String normalizeString(String str) {
        if (str == null) return "";
        // Trim first to remove leading/trailing spaces
        String trimmed = str.trim();
        String clean = trimmed.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove diacritical marks
    }

    public void deletePointage(String documentId, OnDeleteListener listener) {
        String filter = "id_pointage=eq." + documentId;

        connectClient.delete("pointage", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                if (listener != null) listener.onSuccess();
                loadHistorique();
            }

            @Override
            public void onError(Exception error) {
                if (listener != null) listener.onFailure(error);
            }
        });
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnHistoriqueLoadedListener {
        void onHistoriqueLoaded(List<Pointage> pointages);
        void onError(Exception e);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (Runnable task : scheduledAbsenceChecks.values()) {
            absenceCheckHandler.removeCallbacks(task);
        }
        scheduledAbsenceChecks.clear();
    }

    public LiveData<List<Pointage>> getHistorique() {
        return historiqueLiveData;
    }

    private Calendar getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Force la vérification immédiate des absences pour debug
     * Utile pour déclencher manuellement la détection
     */
    public void forceCheckAbsences() {
        Log.i("HistoriqueViewModel", "=== FORÇAGE VERIFICATION DES ABSENCES ===");
        checkSessionAbsences();
    }

    /**
     * Vérifie et marque automatiquement les absences pour les examens passés seulement
     * Un surveillant ne peut être absent que pour un examen déjà terminé
     */
    public void checkSessionAbsences() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());
        // Limite: 60 jours en arrière
        cal.add(Calendar.DAY_OF_MONTH, -60);
        String startStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());

        Log.i("HistoriqueViewModel", "=== DÉBUT VÉRIFICATION DES ABSENCES ===");
        Log.i("HistoriqueViewModel", "Période vérifiée: " + startStr + " à " + todayStr);
        Log.i("HistoriqueViewModel", "Heure actuelle: " + now);

        // Charger les plannings depuis startStr jusqu'à aujourd'hui inclus
        String planningFilter = "date_examen=gte." + startStr + "&date_examen=lte." + todayStr;
        Log.d("HistoriqueViewModel", "Filtre planning: " + planningFilter);

        connectClient.select("planning_surveillance", "*", planningFilter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                Log.i("HistoriqueViewModel", "Récupéré " + planningResult.size() + " entrées de planning à traiter");
                try {
                    for (int i = 0; i < planningResult.size(); i++) {
                        JsonObject planning = planningResult.get(i).getAsJsonObject();
                        if (!planning.has("id_surveillant") || planning.get("id_surveillant").isJsonNull()) continue;
                        if (!planning.has("date_examen") || planning.get("date_examen").isJsonNull()) continue;

                        Long surveillantId = planning.get("id_surveillant").getAsLong();
                        String dateExamenStr = planning.get("date_examen").getAsString();
                        String numeroSalle = planning.has("numero_salle") && !planning.get("numero_salle").isJsonNull() ? planning.get("numero_salle").getAsString() : null;
                        String heureDebutStr = planning.has("heure_debut") && !planning.get("heure_debut").isJsonNull() ? planning.get("heure_debut").getAsString() : null;
                        String heureFinStr = planning.has("heure_fin") && !planning.get("heure_fin").isJsonNull() ? planning.get("heure_fin").getAsString() : null;

                        try {
                            Date examDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateExamenStr);
                            Calendar examDay = getStartOfDay(examDate);
                            boolean isPastDay = examDay.getTime().before(getStartOfDay(now).getTime());

                            // Déterminer la session et l'heure de fin effective
                            String session;
                            Date endTime;
                            if (heureDebutStr != null) {
                                Date debut = DateUtils.parseSupabaseTimestamp(heureDebutStr);
                                session = getSession(debut);
                            } else if (heureFinStr != null) {
                                Date fin = DateUtils.parseSupabaseTimestamp(heureFinStr);
                                session = getSession(fin);
                            } else {
                                // Défaut: matin
                                session = "Matin";
                            }

                            if (heureFinStr != null) {
                                endTime = DateUtils.parseSupabaseTimestamp(heureFinStr);
                            } else {
                                // Si pas d'heure_fin, utiliser borne de session (12:00 ou 18:00) du jour exam
                                Calendar endCal = (Calendar) examDay.clone();
                                endCal.set(Calendar.HOUR_OF_DAY, session.equals("Matin") ? 12 : 18);
                                endCal.set(Calendar.MINUTE, 0);
                                endCal.set(Calendar.SECOND, 0);
                                endTime = endCal.getTime();
                            }

                            // Ne marquer absent que si l'examen est passé
                            // Un examen est considéré comme passé si :
                            // 1. C'est un jour passé (pas aujourd'hui) OU
                            // 2. C'est aujourd'hui mais l'heure de fin est dépassée
                            boolean examFinished = isPastDay || now.after(endTime);
                            if (!examFinished) {
                                Log.d("HistoriqueViewModel", "Examen non terminé - Surveillant: " + surveillantId + ", Date: " + dateExamenStr + ", Session: " + session + ", isPastDay: " + isPastDay + ", now: " + now + ", endTime: " + endTime);
                                continue; // Pas encore fini
                            }

                            Log.d("HistoriqueViewModel", "Vérification absence pour Surveillant: " + surveillantId + ", Date: " + dateExamenStr + ", Session: " + session + ", Salle: " + numeroSalle);

                            // Vérifier s'il y a un pointage ce jour-là pour cette session
                            String dayStart = dateExamenStr + "T00:00:00";
                            String dayEnd = dateExamenStr + "T23:59:59";
                            String pointageFilter = "id_surveillant=eq." + surveillantId
                                    + "&heure_pointage=gte." + dayStart
                                    + "&heure_pointage=lte." + dayEnd;

                            final String sessionFinal = session;
                            connectClient.select("pointage", "*", pointageFilter, new ConnectClient.ClientCallback() {
                                @Override
                                public void onSuccess(JsonArray pointageResult) {
                                    boolean hasPointageInSession = false;
                                    try {
                                        for (int j = 0; j < pointageResult.size(); j++) {
                                            JsonObject p = pointageResult.get(j).getAsJsonObject();
                                            String ts = p.get("heure_pointage").getAsString();
                                            Date pt = DateUtils.parseSupabaseTimestamp(ts);
                                            if (getSession(pt).equals(sessionFinal)) {
                                                hasPointageInSession = true;
                                                break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("HistoriqueViewModel", "Erreur vérif pointages", e);
                                    }

                                    if (!hasPointageInSession) {
                                        Log.w("HistoriqueViewModel", "ABSENCE DÉTECTÉE - Surveillant ID: " + surveillantId + ", Date: " + dateExamenStr + ", Session: " + sessionFinal + ", Salle: " + numeroSalle);
                                        // Récupérer le nom du surveillant
                                        connectClient.select("surveillant", "nom_surveillant", "id_surveillant=eq." + surveillantId, new ConnectClient.ClientCallback() {
                                            @Override
                                            public void onSuccess(JsonArray surveillantResult) {
                                                if (surveillantResult.size() > 0) {
                                                    String nomSurveillant = surveillantResult.get(0).getAsJsonObject().get("nom_surveillant").getAsString();
                                                    // Enregistrer l'absence immédiatement
                                                    checkAndInsertSanction(surveillantId, "ABSENCE", dateExamenStr, numeroSalle, sessionFinal, nomSurveillant, new ConnectClient.ClientCallback() {
                                                        @Override
                                                        public void onSuccess(JsonArray result) {
                                                            Log.i("HistoriqueViewModel", "✅ ABSENCE ENREGISTRÉE: " + nomSurveillant + " - " + sessionFinal + " - Salle " + numeroSalle + " - " + dateExamenStr);
                                                            // ✅ Afficher notification de l'absence (une seule fois, persistante après redémarrage)
                                                            final String absenceDate = dateExamenStr;
                                                            NotificationHelper.showAbsenceNotification(surveillantId, nomSurveillant, numeroSalle, sessionFinal, absenceDate);
                                                            // ✅ Envoyer email de notification d'absence
                                                            Log.i("HistoriqueViewModel", "📧 DÉCLENCHEMENT ENVOI EMAIL pour absence: " + nomSurveillant + " (ID: " + surveillantId + ")");
                                                            //fetchSurveillantEmailAndSendNotification(surveillantId, nomSurveillant, absenceDate, "ABSENCE");
                                                        }



                                                        @Override
                                                        public void onError(Exception e) {
                                                            Log.e("HistoriqueViewModel", "❌ ERREUR enregistrement absence: " + nomSurveillant, e);
                                                        }
                                                    });
                                                } else {
                                                    Log.w("HistoriqueViewModel", "Surveillant introuvable pour ID: " + surveillantId);
                                                }
                                            }

                                            @Override
                                            public void onError(Exception error) {
                                                Log.e("HistoriqueViewModel", "Erreur récupération nom surveillant pour ID: " + surveillantId, error);
                                            }
                                        });
                                    } else {
                                        Log.d("HistoriqueViewModel", "Pointage détecté - Surveillant ID: " + surveillantId + ", Session: " + sessionFinal + ", Date: " + dateExamenStr);
                                    }
                                }

                                @Override
                                public void onError(Exception error) {
                                    Log.e("HistoriqueViewModel", "Erreur chargement pointage pour absence", error);
                                }
                            });
                        } catch (Exception e) {
                            Log.e("HistoriqueViewModel", "Erreur traitement planning passé", e);
                        }
                    }
                } catch (Exception e) {
                    Log.e("HistoriqueViewModel", "Erreur lors du traitement du planning pour absences", e);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("HistoriqueViewModel", "Erreur lors de la récupération du planning pour absences", error);
            }
        });
    }
}
