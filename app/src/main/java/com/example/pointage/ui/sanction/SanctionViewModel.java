package com.example.pointage.ui.sanction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ui.historique.Pointage;
import com.example.pointage.ui.historique.DateUtils;
import com.example.pointage.SupabaseClient;
import com.example.pointage.utils.NotificationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.content.Context;
import com.google.gson.Gson;

public class SanctionViewModel extends ViewModel {

    private final MutableLiveData<List<SurveillantSanction>> sanctionLiveData = new MutableLiveData<>();
    private final SupabaseClient supabaseClient = SupabaseClient.getInstance();
    private SharedPreferences sharedPreferences;

    private static final String TAG = "SanctionVM";
    private static final String PREFS_NAME = "sanction_prefs";
    private static final String KEY_NOTIFIED_SANCTIONS = "notified_sanctions";

    // Empêche les rafraîchissements qui se chevauchent
    private volatile boolean isLoading = false;
    private volatile boolean isInitialLoad = true;
    private int selectedMonth;
    private int selectedYear;

    // Suivi des sanctions déjà notifiées
    private final Set<String> notifiedSanctions = new HashSet<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final long refreshIntervalMs = 15_000; // 15s: ajustez selon besoin
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            loadSanctionsInternal(selectedMonth, selectedYear, false); // false = pas le chargement initial
            // Re-planifie la prochaine exécution
            refreshHandler.postDelayed(this, refreshIntervalMs);
        }
    };

    public SanctionViewModel() throws UnknownHostException {
        // Charger les sanctions notifiées depuis SharedPreferences
        loadNotifiedSanctionsFromPrefs();

        Calendar currentCalendar = Calendar.getInstance();
        selectedMonth = currentCalendar.get(Calendar.MONTH);
        selectedYear = currentCalendar.get(Calendar.YEAR);
        loadSanctionsInternal(selectedMonth, selectedYear, true); // true = chargement initial
        // Démarrer l'auto-refresh
        refreshHandler.postDelayed(refreshRunnable, refreshIntervalMs);
    }

    public void loadSanctions(int month, int year) {
        selectedMonth = month;
        selectedYear = year;
        loadSanctionsInternal(month, year, false); // Par défaut, c'est un rafraîchissement (pas initial)
    }

    private void loadSanctionsInternal(int month, int year, boolean shouldNotifyOnFirstLoad) {
        if (isLoading) {
            Log.d(TAG, "Skip refresh: still loading");
            return;
        }
        isLoading = true;

        // Mettez à jour le drapeau isInitialLoad seulement lors du premier appel
        if (shouldNotifyOnFirstLoad && isInitialLoad) {
            // C'est un chargement initial, il ne doit pas déclencher de notifications
        }

        Map<String, SurveillantSanction> sanctionsMap = new HashMap<>();
        final Map<Long, String> surveillantRoomById = new HashMap<>();
        final Map<Long, String> surveillantNameById = new HashMap<>();
        final Set<String> validPlannedSurveillants = new HashSet<>(); // (surveillantId|id_examen) valides

        // 1️⃣ Charger d'abord les planifications valides
        supabaseClient.select("planning_surveillance", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                try {
                    for (int i = 0; i < planningResult.size(); i++) {
                        JsonObject planning = planningResult.get(i).getAsJsonObject();
                        if (!planning.has("id_surveillant") || planning.get("id_surveillant").isJsonNull()) {
                            continue;
                        }
                        Long surveillantId = planning.get("id_surveillant").getAsLong();
                        // Include id_examen in the key to match against specific exams
                        Long idExamen = planning.has("id_examen") && !planning.get("id_examen").isJsonNull()
                                ? planning.get("id_examen").getAsLong() : null;
                        if (idExamen != null) {
                            String key = surveillantId + "|" + idExamen;
                            validPlannedSurveillants.add(key);
                        }
                    }
                    Log.d(TAG, "Planifications validées: " + validPlannedSurveillants.size());

                    // Continuer avec le chargement des surveillants
                    loadSurveillants(month, year, sanctionsMap, surveillantRoomById, surveillantNameById, validPlannedSurveillants);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du chargement du planning_surveillance", e);
                    isLoading = false;
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Erreur lors du chargement du planning_surveillance", error);
                isLoading = false;
            }
        });
    }

    private void loadSurveillants(int month, int year, Map<String, SurveillantSanction> sanctionsMap,
                                  Map<Long, String> surveillantRoomById, Map<Long, String> surveillantNameById,
                                  Set<String> validPlannedSurveillants) {
        // 2️⃣ Récupérer tous les surveillants
        supabaseClient.select("surveillant", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray surveillantResult) {
                try {
                    for (int i = 0; i < surveillantResult.size(); i++) {
                        JsonObject surveillantDoc = surveillantResult.get(i).getAsJsonObject();
                        Long surveillantId = surveillantDoc.get("id_surveillant").getAsLong();
                        String nomSurveillant = surveillantDoc.get("nom_surveillant").getAsString();
                        String numeroSalle = surveillantDoc.has("numero_salle") && !surveillantDoc.get("numero_salle").isJsonNull()
                                ? surveillantDoc.get("numero_salle").getAsString() : null;

                        if (surveillantId != null && nomSurveillant != null) {
                            String key = surveillantId + "_" + month;
                            sanctionsMap.put(key, new SurveillantSanction(nomSurveillant, month, year));
                            surveillantNameById.put(surveillantId, nomSurveillant);
                            if (numeroSalle != null) {
                                surveillantRoomById.put(surveillantId, numeroSalle);
                            }
                        }
                    }

                    // 3️⃣ Récupérer tous les examens du mois
                    supabaseClient.select("examen", "*", null, new SupabaseClient.SupabaseCallback() {
                        @Override
                        public void onSuccess(JsonArray examResult) {
                            List<JsonObject> examDocs = new ArrayList<>();

                            try {
                                for (int i = 0; i < examResult.size(); i++) {
                                    JsonObject examDoc = examResult.get(i).getAsJsonObject();

                                    if (!examDoc.has("heure_debut") || examDoc.get("heure_debut").isJsonNull()) continue;
                                    String heureDebutStrForFilter = examDoc.get("heure_debut").getAsString();

                                    Date heureDebutForFilter;
                                    try {
                                        heureDebutForFilter = DateUtils.parseSupabaseTimestamp(heureDebutStrForFilter);
                                    } catch (ParseException pe) {
                                        Log.w(TAG, "Failed to parse heure_debut (filter): " + heureDebutStrForFilter, pe);
                                        continue;
                                    }

                                    Calendar examDay = getStartOfDay(heureDebutForFilter);

                                    if (examDay.get(Calendar.MONTH) == month && examDay.get(Calendar.YEAR) == year) {
                                        examDocs.add(examDoc);
                                    }
                                }

                                if (examDocs.isEmpty()) {
                                    // Pas d'examens, vérifier les absences via planning_surveillance
                                    checkAbsencesFromPlanning(month, year, sanctionsMap, surveillantNameById);
                                    return;
                                }

                                // 4️⃣ Récupérer tous les pointages
                                supabaseClient.select("pointage", "*", null, new SupabaseClient.SupabaseCallback() {
                                    @Override
                                    public void onSuccess(JsonArray pointageResult) {
                                        Map<Long, Map<String, List<Pointage>>> pointagesByDayAndSession = new HashMap<>();
                                        List<SanctionToSave> sanctionsToSave = new ArrayList<>();
                                        final java.util.Set<String> uniqueSanctionKeys = new java.util.HashSet<>();
                                        // Compteur d'absences par surveillant et par jour pour limiter à 2 max
                                        final Map<String, Integer> dailyAbsencesCount = new HashMap<>();

                                        try {
                                            for (int i = 0; i < pointageResult.size(); i++) {
                                                JsonObject pointageDoc = pointageResult.get(i).getAsJsonObject();
                                                Pointage pointage = new Pointage();
                                                pointage.setId_surveillant(pointageDoc.get("id_surveillant").getAsLong());

                                                Date hp;
                                                try {
                                                    String ts = pointageDoc.get("heure_pointage").getAsString();
                                                    hp = DateUtils.parseSupabaseTimestamp(ts);
                                                } catch (Exception pe) {
                                                    continue;
                                                }
                                                pointage.setHeure_pointage(hp);
                                                pointage.setRetard(pointageDoc.get("retard").getAsBoolean());

                                                Calendar pointageDay = getStartOfDay(pointage.getHeure_pointage());
                                                int pointageMonth = pointageDay.get(Calendar.MONTH);
                                                int pointageYear = pointageDay.get(Calendar.YEAR);
                                                if (pointageMonth != month || pointageYear != year) continue;

                                                Calendar pHourCal = Calendar.getInstance();
                                                pHourCal.setTime(pointage.getHeure_pointage());
                                                int hour = pHourCal.get(Calendar.HOUR_OF_DAY);
                                                String sessionPointage = (hour < 12) ? "Matin" : "Après-midi";

                                                Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession
                                                        .getOrDefault(pointageDay.getTimeInMillis(), new HashMap<>());
                                                List<Pointage> listForSession = sessionMap.getOrDefault(sessionPointage, new ArrayList<>());
                                                listForSession.add(pointage);
                                                sessionMap.put(sessionPointage, listForSession);
                                                pointagesByDayAndSession.put(pointageDay.getTimeInMillis(), sessionMap);
                                            }

                                            // 5️⃣ Calcul des absences et retards par examen
                                            for (JsonObject examDoc : examDocs) {
                                                String heureDebutStr = examDoc.get("heure_debut").getAsString();
                                                Date heureDebutTime;
                                                try {
                                                    heureDebutTime = DateUtils.parseSupabaseTimestamp(heureDebutStr);
                                                } catch (ParseException e) {
                                                    Log.w(TAG, "Failed to parse heure_debut: " + heureDebutStr, e);
                                                    continue;
                                                }

                                                Calendar examDay = getStartOfDay(heureDebutTime);

                                                Date heureFinTime = heureDebutTime;
                                                if (examDoc.has("heure_fin") && !examDoc.get("heure_fin").isJsonNull()) {
                                                    try {
                                                        Date heureFinParsed = DateUtils.parseTime(examDoc.get("heure_fin").getAsString());
                                                        Calendar finCal = Calendar.getInstance();
                                                        finCal.setTime(heureFinParsed);
                                                        finCal.set(examDay.get(Calendar.YEAR), examDay.get(Calendar.MONTH), examDay.get(Calendar.DAY_OF_MONTH));
                                                        heureFinTime = finCal.getTime();
                                                    } catch (Exception e) {
                                                        Log.w(TAG, "Failed to parse heure_fin, using heure_debut", e);
                                                    }
                                                }

                                                Calendar heureDebCal = Calendar.getInstance();
                                                heureDebCal.setTime(heureDebutTime);
                                                String examSession = (heureDebCal.get(Calendar.HOUR_OF_DAY) < 12) ? "Matin" : "Après-midi";

                                                // IMPORTANT: Ne marquer absent que si l'examen est déjà terminé
                                                Date now = new Date();
                                                if (heureFinTime.after(now)) {
                                                    Log.d(TAG, "Examen pas encore terminé: " + heureDebutStr + " - " + heureFinTime + ", ne pas calculer d'absence");
                                                    continue; // Examen pas encore terminé, pas d'absence possible
                                                }

                                                // Get exam ID for planning validation
                                                Long idExamen = examDoc.has("id_examen") && !examDoc.get("id_examen").isJsonNull()
                                                        ? examDoc.get("id_examen").getAsLong() : null;

                                                for (Map.Entry<String, SurveillantSanction> entry : sanctionsMap.entrySet()) {
                                                    SurveillantSanction sanction = entry.getValue();
                                                    Long surveillantId = Long.parseLong(entry.getKey().split("_")[0]);

                                                    String surveillantRoom = surveillantRoomById.get(surveillantId);
                                                    String examRoom = examDoc.has("numero_salle") && !examDoc.get("numero_salle").isJsonNull()
                                                            ? examDoc.get("numero_salle").getAsString() : null;

                                                    String sRoom = normalizeRoom(surveillantRoom);
                                                    String eRoom = normalizeRoom(examRoom);
                                                    boolean roomsMatch = (sRoom != null && eRoom != null && sRoom.equalsIgnoreCase(eRoom));

                                                    boolean hasPointage = false;
                                                    if (roomsMatch && pointagesByDayAndSession.containsKey(examDay.getTimeInMillis())) {
                                                        Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession.get(examDay.getTimeInMillis());
                                                        if (sessionMap.containsKey(examSession)) {
                                                            for (Pointage p : sessionMap.get(examSession)) {
                                                                if (p.getId_surveillant().equals(surveillantId)) {
                                                                    hasPointage = true;

                                                                    Calendar pCal = Calendar.getInstance();
                                                                    pCal.setTime(p.getHeure_pointage());

                                                                    Calendar debutCalSameDay = (Calendar) examDay.clone();
                                                                    debutCalSameDay.set(Calendar.HOUR_OF_DAY, heureDebCal.get(Calendar.HOUR_OF_DAY));
                                                                    debutCalSameDay.set(Calendar.MINUTE, heureDebCal.get(Calendar.MINUTE));
                                                                    debutCalSameDay.set(Calendar.SECOND, heureDebCal.get(Calendar.SECOND));

                                                                    if (pCal.after(debutCalSameDay)) {
                                                                        // Compter le retard et le persister
                                                                        sanction.addRetard();
                                                                        sanctionsToSave.add(new SanctionToSave(
                                                                                surveillantId, "RETARD", examDay.getTime(),
                                                                                surveillantNameById.get(surveillantId), examRoom, examSession
                                                                        ));
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Absence uniquement si:
                                                    // 1. La salle correspond
                                                    // 2. Aucun pointage
                                                    // 3. Le surveillant est programmé pour CET examen spécifique dans planning_surveillance
                                                    if (roomsMatch && !hasPointage) {
                                                        String planningKey = surveillantId + "|" + idExamen;
                                                        boolean isScheduledForThisExam = idExamen != null && validPlannedSurveillants.contains(planningKey);

                                                        if (isScheduledForThisExam) {
                                                            // Règles:
                                                            // - Au plus 1 absence par (jour, session) pour ce surveillant
                                                            // - Au plus 2 absences au total dans la journée (Matin + Après-midi)
                                                            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(examDay.getTime());
                                                            String dailyKey = surveillantId + "|" + dateStr;
                                                            int currentDailyAbs = dailyAbsencesCount.getOrDefault(dailyKey, 0);
                                                            if (currentDailyAbs < 2) {
                                                                String key = surveillantId + "|ABSENCE|" + dateStr + "|" + examSession; // session = "Matin" ou "Après-midi"
                                                                if (!uniqueSanctionKeys.contains(key)) {
                                                                    uniqueSanctionKeys.add(key);
                                                                    sanction.addAbsence();
                                                                    sanctionsToSave.add(new SanctionToSave(
                                                                            surveillantId, "ABSENCE", examDay.getTime(),
                                                                            surveillantNameById.get(surveillantId), examRoom, examSession
                                                                    ));
                                                                    // Incrémenter le compteur journalier
                                                                    dailyAbsencesCount.put(dailyKey, currentDailyAbs + 1);
                                                                }
                                                            }
                                                        } else {
                                                            Log.d(TAG, "Surveillant " + surveillantId + " pas planifié pour examen " + idExamen + ", absence non enregistrée");
                                                        }
                                                    }
                                                }
                                            }

                                            // 6️⃣ Vérifier aussi les absences via planning_surveillance (pour les cas non couverts par examen)
                                            checkAbsencesFromPlanningAdditional(month, year, sanctionsToSave, surveillantNameById, pointagesByDayAndSession);

                                            // 7️⃣ Sauvegarder les sanctions dans la table sanction (en évitant les doublons)
                                            saveSanctionsToDatabase(sanctionsToSave, month, year, sanctionsMap, surveillantNameById);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // En cas d'erreur, charger les sanctions existantes
                                            loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                                        }
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        error.printStackTrace();
                                        // En cas d'erreur, charger les sanctions existantes
                                        loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                            } finally {
                                isLoading = false;
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            error.printStackTrace();
                            loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                            isLoading = false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    sanctionLiveData.setValue(new ArrayList<>());
                } finally {
                    isLoading = false;
                }
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                sanctionLiveData.setValue(new ArrayList<>());
                isLoading = false;
            }
        });
    }

    private void saveSanctionsToDatabase(List<SanctionToSave> sanctionsToSave, int month, int year,
                                         Map<String, SurveillantSanction> sanctionsMap,
                                         Map<Long, String> surveillantNameById) {
        if (sanctionsToSave.isEmpty()) {
            // Marquer que le chargement initial est fait même s'il n'y a pas de sanctions à sauvegarder
            if (isInitialLoad) {
                this.isInitialLoad = false;
                Log.d(TAG, "Chargement initial terminé (pas de sanctions), notifications activées pour les rafraîchissements futurs");
            }
            loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
            return;
        }

        // Sauvegarder chaque sanction dans la table sanction, puis ne recharger qu'une fois toutes les insertions terminées
        final int total = sanctionsToSave.size();
        final int[] completed = {0};

        for (SanctionToSave sanction : sanctionsToSave) {
            JsonObject sanctionData = new JsonObject();
            sanctionData.addProperty("id_surveillant", sanction.surveillantId);
            sanctionData.addProperty("type", sanction.type);
            sanctionData.addProperty("date_examen", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sanction.examDate));
            sanctionData.addProperty("nom_surveillant", sanction.surveillantName);
            sanctionData.addProperty("numero_salle", sanction.roomNumber);
            // date_creation comme TIME (HH:mm:ss) pas timestamp complet
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            sanctionData.addProperty("date_creation", currentTime);
            // Ajouter la session si disponible
            if (sanction.session != null) {
                sanctionData.addProperty("session", sanction.session);
            }

            // Les notifications sont maintenant gérées par HistoriqueViewModel au moment du scan
            // pour éviter les doublons et une meilleure UX immédiate
            Log.d(TAG, "Sanction détectée: " + sanction.type + " pour " + sanction.surveillantName);

            supabaseClient.insert("sanction", sanctionData, new SupabaseClient.SupabaseCallback() {
                private void done() {
                    synchronized (completed) {
                        completed[0]++;
                        if (completed[0] >= total) {
                            // Marquer que le chargement initial est fait
                            if (SanctionViewModel.this.isInitialLoad) {
                                SanctionViewModel.this.isInitialLoad = false;
                                Log.d(TAG, "Chargement initial terminé, notifications activées pour les rafraîchissements futurs");
                            }
                            // Après toutes les insertions, recharger depuis la table
                            loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                        }
                    }
                }

                @Override
                public void onSuccess(JsonArray result) {
                    Log.d(TAG, "Sanction enregistrée avec succès");
                    done();
                }

                @Override
                public void onError(Exception error) {
                    // 409 doublon attendu possible (grâce au header resolution=ignore-duplicates)
                    Log.e(TAG, "Erreur lors de l'enregistrement de la sanction", error);
                    done();
                }
            });
        }

        // Si aucun insert (déjà géré plus haut par early return), rien à faire ici
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Stopper l'auto-refresh pour éviter les fuites
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void loadExistingSanctions(int month, int year, Map<String, SurveillantSanction> sanctionsMap,
                                       Map<Long, String> surveillantNameById) {
        // Réinitialiser les absences et retards: tout sera rechargé depuis la table
        for (SurveillantSanction sanction : sanctionsMap.values()) {
            sanction.reset();
        }

        // Charger les sanctions depuis la table sanction pour le mois/année donné
        String monthStr = String.format(Locale.getDefault(), "%02d", month + 1);
        String yearStr = String.valueOf(year);
        String startDate = yearStr + "-" + monthStr + "-01";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        String nextMonthStr = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1);
        String nextYearStr = String.valueOf(cal.get(Calendar.YEAR));
        String nextStart = nextYearStr + "-" + nextMonthStr + "-01";

        String sanctionFilter = "date_examen=gte." + startDate + "&date_examen=lt." + nextStart;

        supabaseClient.select("sanction", "*", sanctionFilter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray sanctionResult) {
                try {
                    for (int i = 0; i < sanctionResult.size(); i++) {
                        JsonObject s = sanctionResult.get(i).getAsJsonObject();
                        if (!s.has("id_surveillant") || s.get("id_surveillant").isJsonNull()) continue;

                        Long idSurv = s.get("id_surveillant").getAsLong();
                        String type = s.has("type") && !s.get("type").isJsonNull() ? s.get("type").getAsString() : "";

                        String key = idSurv + "_" + month;
                        SurveillantSanction agg = sanctionsMap.get(key);

                        if (agg == null) {
                            // Ne pas créer d'entrée pour les surveillants supprimés
                            // Si le surveillant n'existe plus dans la table surveillant, ignorer cette sanction
                            Log.d(TAG, "Ignoring sanction for deleted surveillant ID: " + idSurv);
                            continue;
                        }

                        // 🔔 Marquer la sanction comme déjà notifiée (chargée depuis la base)
                        String dateExamen = s.has("date_examen") && !s.get("date_examen").isJsonNull()
                                ? s.get("date_examen").getAsString() : "";
                        String session = s.has("session") && !s.get("session").isJsonNull()
                                ? s.get("session").getAsString() : "";
                        String notificationKey = idSurv + "_" + type + "_" + dateExamen + "_" + session;
                        notifiedSanctions.add(notificationKey);

                        if ("RETARD".equalsIgnoreCase(type)) {
                            agg.addRetard();
                        } else if ("ABSENCE".equalsIgnoreCase(type)) {
                            agg.addAbsence();
                        }
                    }











                    // ✅ Sauvegarder les sanctions chargées dans SharedPreferences
                    saveNotifiedSanctionsToPrefs();

                    // Finaliser la liste avec tous les surveillants
                    sanctionLiveData.setValue(new ArrayList<>(sanctionsMap.values()));
                    isLoading = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    sanctionLiveData.setValue(new ArrayList<>(sanctionsMap.values()));
                }
            }

            @Override
            public void onError(Exception error) {
                // En cas d'erreur, retourner la liste des surveillants sans sanctions
                sanctionLiveData.setValue(new ArrayList<>(sanctionsMap.values()));
            }
        });
    }

    private Calendar getStartOfDay(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    // Normalize room identifiers (trim, remove common prefixes like "salle",
    // replace multiple spaces, uppercase)
    private String normalizeRoom(String room) {
        if (room == null) return null;
        String r = room.trim();
        // Remove "Salle" or "SALLE" prefix if present
        r = r.replaceAll("(?i)^salle\\s*", "");
        // Collapse spaces and dashes
        r = r.replaceAll("\\s+", " ").replace("-", " ");
        return r.toUpperCase(Locale.getDefault());
    }

    public LiveData<List<SurveillantSanction>> getSanctions() {
        return sanctionLiveData;
    }

    // Classe interne pour représenter une sanction à sauvegarder
    private static class SanctionToSave {
        Long surveillantId;
        String type;
        Date examDate;
        String surveillantName;
        String roomNumber;
        String session;

        SanctionToSave(Long surveillantId, String type, Date examDate, String surveillantName, String roomNumber) {
            this.surveillantId = surveillantId;
            this.type = type;
            this.examDate = examDate;
            this.surveillantName = surveillantName;
            this.roomNumber = roomNumber;
            this.session = null;
        }

        SanctionToSave(Long surveillantId, String type, Date examDate, String surveillantName, String roomNumber, String session) {
            this.surveillantId = surveillantId;
            this.type = type;
            this.examDate = examDate;
            this.surveillantName = surveillantName;
            this.roomNumber = roomNumber;
            this.session = session;
        }
    }

    /**
     * Vérifie les absences via planning_surveillance quand il n'y a pas d'examens
     */
    private void checkAbsencesFromPlanning(int month, int year, Map<String, SurveillantSanction> sanctionsMap, Map<Long, String> surveillantNameById) {
        // Construire la plage de dates pour le mois
        String monthStr = String.format(Locale.getDefault(), "%02d", month + 1);
        String yearStr = String.valueOf(year);
        String startDate = yearStr + "-" + monthStr + "-01";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        String nextMonthStr = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1);
        String nextYearStr = String.valueOf(cal.get(Calendar.YEAR));
        String nextStart = nextYearStr + "-" + nextMonthStr + "-01";

        String planningFilter = "date_examen=gte." + startDate + "&date_examen=lt." + nextStart;

        supabaseClient.select("planning_surveillance", "*", planningFilter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                if (planningResult.size() == 0) {
                    // Pas de planning, charger les sanctions existantes
                    loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                    return;
                }

                // Charger les pointages pour le mois
                String pointageStartDate = startDate + "T00:00:00";
                String pointageEndDate = nextStart + "T00:00:00";
                String pointageFilter = "heure_pointage=gte." + pointageStartDate + "&heure_pointage=lt." + pointageEndDate;

                supabaseClient.select("pointage", "*", pointageFilter, new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(JsonArray pointageResult) {
                        processPlanningAbsences(planningResult, pointageResult, month, year, sanctionsMap, surveillantNameById);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Erreur lors du chargement des pointages pour planning", error);
                        loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Erreur lors du chargement du planning_surveillance", error);
                loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
            }
        });
    }

    /**
     * Vérifie les absences supplémentaires via planning_surveillance après le traitement des examens
     */
    private void checkAbsencesFromPlanningAdditional(int month, int year, List<SanctionToSave> sanctionsToSave,
                                                     Map<Long, String> surveillantNameById,
                                                     Map<Long, Map<String, List<Pointage>>> pointagesByDayAndSession) {
        // Construire la plage de dates pour le mois
        String monthStr = String.format(Locale.getDefault(), "%02d", month + 1);
        String yearStr = String.valueOf(year);
        String startDate = yearStr + "-" + monthStr + "-01";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        String nextMonthStr = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1);
        String nextYearStr = String.valueOf(cal.get(Calendar.YEAR));
        String nextStart = nextYearStr + "-" + nextMonthStr + "-01";

        String planningFilter = "date_examen=gte." + startDate + "&date_examen=lt." + nextStart;

        supabaseClient.select("planning_surveillance", "*", planningFilter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                processAdditionalPlanningAbsences(planningResult, sanctionsToSave, surveillantNameById, pointagesByDayAndSession);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Erreur lors du chargement du planning_surveillance additionnel", error);
            }
        });
    }

    /**
     * Traite les absences basées sur planning_surveillance
     */
    private void processPlanningAbsences(JsonArray planningResult, JsonArray pointageResult, int month, int year,
                                         Map<String, SurveillantSanction> sanctionsMap, Map<Long, String> surveillantNameById) {
        // Construire une map des pointages par surveillant, jour et session
        Map<Long, Map<String, List<Pointage>>> pointagesByDayAndSession = new HashMap<>();

        try {
            for (int i = 0; i < pointageResult.size(); i++) {
                JsonObject pointageDoc = pointageResult.get(i).getAsJsonObject();
                Pointage pointage = new Pointage();
                pointage.setId_surveillant(pointageDoc.get("id_surveillant").getAsLong());

                Date hp;
                try {
                    String ts = pointageDoc.get("heure_pointage").getAsString();
                    hp = DateUtils.parseSupabaseTimestamp(ts);
                } catch (Exception pe) {
                    continue;
                }
                pointage.setHeure_pointage(hp);

                Calendar pointageDay = getStartOfDay(pointage.getHeure_pointage());
                Calendar pHourCal = Calendar.getInstance();
                pHourCal.setTime(pointage.getHeure_pointage());
                int hour = pHourCal.get(Calendar.HOUR_OF_DAY);
                String sessionPointage = (hour < 12) ? "Matin" : "Après-midi";

                Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession
                        .getOrDefault(pointageDay.getTimeInMillis(), new HashMap<>());
                List<Pointage> listForSession = sessionMap.getOrDefault(sessionPointage, new ArrayList<>());
                listForSession.add(pointage);
                sessionMap.put(sessionPointage, listForSession);
                pointagesByDayAndSession.put(pointageDay.getTimeInMillis(), sessionMap);
            }

            List<SanctionToSave> sanctionsToSave = new ArrayList<>();
            processAdditionalPlanningAbsences(planningResult, sanctionsToSave, surveillantNameById, pointagesByDayAndSession);

            // Sauvegarder les sanctions détectées
            saveSanctionsToDatabase(sanctionsToSave, month, year, sanctionsMap, surveillantNameById);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement des absences par planning", e);
            loadExistingSanctions(month, year, sanctionsMap, surveillantNameById);
        }
    }

    /**
     * Traite les absences supplémentaires basées sur planning_surveillance
     */
    private void processAdditionalPlanningAbsences(JsonArray planningResult, List<SanctionToSave> sanctionsToSave,
                                                   Map<Long, String> surveillantNameById,
                                                   Map<Long, Map<String, List<Pointage>>> pointagesByDayAndSession) {
        final java.util.Set<String> uniqueSanctionKeys = new java.util.HashSet<>();

        try {
            for (int i = 0; i < planningResult.size(); i++) {
                JsonObject planning = planningResult.get(i).getAsJsonObject();

                if (!planning.has("id_surveillant") || planning.get("id_surveillant").isJsonNull()) {
                    continue;
                }

                Long surveillantId = planning.get("id_surveillant").getAsLong();
                String surveillantName = surveillantNameById.get(surveillantId);
                if (surveillantName == null) {
                    continue; // Surveillant supprimé
                }

                String dateExamenStr = planning.get("date_examen").getAsString();
                String numeroSalle = planning.get("numero_salle").getAsString();
                String heureDebutStr = planning.get("heure_debut").getAsString();

                // Parser la date d'examen
                Date dateExamen;
                try {
                    dateExamen = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateExamenStr);
                } catch (ParseException e) {
                    Log.w(TAG, "Impossible de parser date_examen: " + dateExamenStr, e);
                    continue;
                }

                Calendar examDay = getStartOfDay(dateExamen);

                // Déterminer la session basée sur heure_debut
                String session = "Matin"; // par défaut
                try {
                    // Parser l'heure de début pour déterminer la session
                    if (heureDebutStr.length() == 5) { // Format HH:mm
                        String[] timeParts = heureDebutStr.split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        session = (hour < 12) ? "Matin" : "Après-midi";
                    } else if (heureDebutStr.length() == 8) { // Format HH:mm:ss
                        String[] timeParts = heureDebutStr.split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        session = (hour < 12) ? "Matin" : "Après-midi";
                    } else {
                        // Essayer de parser comme timestamp complet
                        Date fullTime = DateUtils.parseSupabaseTimestamp(dateExamenStr + "T" + heureDebutStr);
                        Calendar timeCal = Calendar.getInstance();
                        timeCal.setTime(fullTime);
                        session = (timeCal.get(Calendar.HOUR_OF_DAY) < 12) ? "Matin" : "Après-midi";
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Impossible de parser heure_debut: " + heureDebutStr + ", utilisation session par défaut", e);
                }

                // Vérifier si le surveillant a pointé pour cette session ce jour-là
                boolean hasPointage = false;
                if (pointagesByDayAndSession.containsKey(examDay.getTimeInMillis())) {
                    Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession.get(examDay.getTimeInMillis());
                    if (sessionMap.containsKey(session)) {
                        for (Pointage p : sessionMap.get(session)) {
                            if (p.getId_surveillant().equals(surveillantId)) {
                                hasPointage = true;
                                break;
                            }
                        }
                    }
                }

                // Si pas de pointage, créer une sanction d'absence
                if (!hasPointage) {
                    // Éviter les doublons avec une clé unique
                    String sanctionKey = surveillantId + "|ABSENCE|" + dateExamenStr + "|" + session + "|" + numeroSalle;
                    if (!uniqueSanctionKeys.contains(sanctionKey)) {
                        uniqueSanctionKeys.add(sanctionKey);
                        sanctionsToSave.add(new SanctionToSave(
                                surveillantId, "ABSENCE", dateExamen, surveillantName, numeroSalle, session
                        ));
                        Log.d(TAG, "Absence détectée via planning: " + surveillantName + " - " + session + " - Salle " + numeroSalle + " - " + dateExamenStr);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement des absences supplémentaires par planning", e);
        }
    }

    /**
     * Initialiser SharedPreferences avec le contexte de l'app
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
                        Log.d(TAG, "Loaded " + notifiedSanctions.size() + " previously notified sanctions from SharedPreferences");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du chargement des sanctions notifiées", e);
                }
            }
        }
    }

    /**
     * Sauvegarde les sanctions notifiées dans SharedPreferences
     */
    private void saveNotifiedSanctionsToPrefs() {
        if (sharedPreferences != null) {
            try {
                Gson gson = new Gson();
                String json = gson.toJson(notifiedSanctions.toArray());
                sharedPreferences.edit().putString(KEY_NOTIFIED_SANCTIONS, json).apply();
                Log.d(TAG, "Notified sanctions saved: " + notifiedSanctions.size());
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la sauvegarde des sanctions notifiées", e);
            }
        }
    }
}
