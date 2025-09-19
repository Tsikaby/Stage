package com.example.pointage.ui.sanction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ui.historique.Pointage;
import com.example.pointage.ui.historique.DateUtils;
import com.example.pointage.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

public class SanctionViewModel extends ViewModel {

    private final MutableLiveData<List<SurveillantSanction>> sanctionLiveData = new MutableLiveData<>();
    private final SupabaseClient supabaseClient = SupabaseClient.getInstance();

    private static final String TAG = "SanctionVM";

    public SanctionViewModel() throws UnknownHostException {
        Calendar currentCalendar = Calendar.getInstance();
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        loadSanctions(currentMonth, currentYear);
    }

    public void loadSanctions(int month, int year) {
        Map<String, SurveillantSanction> sanctionsMap = new HashMap<>();
        final Map<Long, String> surveillantRoomById = new HashMap<>();

        // 1️⃣ Récupérer tous les surveillants
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
                            sanctionsMap.put(key, new SurveillantSanction(nomSurveillant, month));
                            if (numeroSalle != null) {
                                surveillantRoomById.put(surveillantId, numeroSalle);
                            }
                        }
                    }

                    // 2️⃣ Récupérer tous les examens du mois
                    // Filtrer les examens par mois/année côté client après récupération
                    supabaseClient.select("examen", "*", null, new SupabaseClient.SupabaseCallback() {
                        @Override
                        public void onSuccess(JsonArray examResult) {
                            List<JsonObject> examDocs = new ArrayList<>();

                            try {
                                for (int i = 0; i < examResult.size(); i++) {
                                    JsonObject examDoc = examResult.get(i).getAsJsonObject();

                                    // heure_debut est un timestamp ISO - utiliser pour le jour et le filtrage du mois/année
                                    if (!examDoc.has("heure_debut") || examDoc.get("heure_debut").isJsonNull()) continue;
                                    String heureDebutStrForFilter = examDoc.get("heure_debut").getAsString();
                                    Log.d(TAG, "Exam raw heure_debut (filter)=" + heureDebutStrForFilter);

                                    Date heureDebutForFilter;
                                    try {
                                        heureDebutForFilter = DateUtils.parseSupabaseTimestamp(heureDebutStrForFilter);
                                        Log.d(TAG, "Exam parsed heure_debut (filter)=" + heureDebutForFilter);
                                    } catch (ParseException pe) {
                                        Log.w(TAG, "Failed to parse heure_debut (filter): " + heureDebutStrForFilter, pe);
                                        continue;
                                    }

                                    Calendar examDay = getStartOfDay(heureDebutForFilter);
                                    Log.d(TAG, "Exam day (from heure_debut) millis=" + examDay.getTimeInMillis());

                                    if (examDay.get(Calendar.MONTH) == month && examDay.get(Calendar.YEAR) == year) {
                                        examDocs.add(examDoc);
                                    }
                                }

                                if (examDocs.isEmpty()) {
                                    sanctionLiveData.setValue(new ArrayList<>());
                                    return;
                                }

                                // 3️⃣ Récupérer tous les pointages
                                supabaseClient.select("pointage", "*", null, new SupabaseClient.SupabaseCallback() {
                                    @Override
                                    public void onSuccess(JsonArray pointageResult) {
                                        // Map<dayMillis, Map<session, List<Pointage>>>
                                        Map<Long, Map<String, List<Pointage>>> pointagesByDayAndSession = new HashMap<>();

                                        try {
                                            for (int i = 0; i < pointageResult.size(); i++) {
                                                JsonObject pointageDoc = pointageResult.get(i).getAsJsonObject();
                                                Pointage pointage = new Pointage();
                                                pointage.setId_surveillant(pointageDoc.get("id_surveillant").getAsLong());

                                                // heure_pointage est ISO ou "yyyy-MM-dd'T'HH:mm:ss"
                                                Date hp;
                                                try {
                                                    String ts = pointageDoc.get("heure_pointage").getAsString();
                                                    hp = DateUtils.parseSupabaseTimestamp(ts);
                                                } catch (Exception pe) {
                                                    continue;
                                                }
                                                pointage.setHeure_pointage(hp);

                                                pointage.setRetard(pointageDoc.get("retard").getAsBoolean());

                                                // Jour du pointage pour regrouper
                                                Calendar pointageDay = getStartOfDay(pointage.getHeure_pointage());
                                                int pointageMonth = pointageDay.get(Calendar.MONTH);
                                                int pointageYear = pointageDay.get(Calendar.YEAR);
                                                if (pointageMonth != month || pointageYear != year) continue;

                                                // Déterminer la session du pointage à partir de l'heure réelle
                                                Calendar pHourCal = Calendar.getInstance();
                                                pHourCal.setTime(pointage.getHeure_pointage());
                                                int hour = pHourCal.get(Calendar.HOUR_OF_DAY);
                                                String sessionPointage = (hour < 12) ? "Matin" : "Après-midi";
                                                Log.d(TAG, "Pointage: id_surveillant=" + pointage.getId_surveillant() + 
                                                        ", raw_ts=" + pointageDoc.get("heure_pointage").getAsString() +
                                                        ", parsed=" + hp + ", session=" + sessionPointage);

                                                Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession
                                                        .getOrDefault(pointageDay.getTimeInMillis(), new HashMap<>());
                                                List<Pointage> listForSession = sessionMap.getOrDefault(sessionPointage, new ArrayList<>());
                                                listForSession.add(pointage);
                                                sessionMap.put(sessionPointage, listForSession);
                                                pointagesByDayAndSession.put(pointageDay.getTimeInMillis(), sessionMap);
                                            }

                                            // 4️⃣ Calcul des absences et retards par examen
                                            for (JsonObject examDoc : examDocs) {
                                                // heure_debut est un timestamp ISO; l'utiliser pour session et jour
                                                String heureDebutStr = examDoc.get("heure_debut").getAsString();
                                                Log.d(TAG, "Exam raw heure_debut=" + heureDebutStr);
                                                Date heureDebutTime;
                                                try {
                                                    heureDebutTime = DateUtils.parseSupabaseTimestamp(heureDebutStr);
                                                    Log.d(TAG, "Exam parsed heure_debut=" + heureDebutTime);
                                                } catch (ParseException e) {
                                                    Log.w(TAG, "Failed to parse heure_debut: " + heureDebutStr, e);
                                                    continue;
                                                }

                                                // Jour d'examen dérivé de heure_debut
                                                Calendar examDay = getStartOfDay(heureDebutTime);
                                                Log.d(TAG, "Per-exam examDay (from heure_debut) millis=" + examDay.getTimeInMillis());

                                                // Calculer l'heure de fin de l'examen
                                                Date heureFinTime = heureDebutTime; // Par défaut, même que debut si pas specifiee
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

                                                // Session de l'examen déterminée par l'heure_debut
                                                Calendar heureDebCal = Calendar.getInstance();
                                                heureDebCal.setTime(heureDebutTime);
                                                String examSession = (heureDebCal.get(Calendar.HOUR_OF_DAY) < 12) ? "Matin" : "Après-midi";

                                                // Ne calculer les absences que pour les examens dont l'heure de fin est dépassée
                                                Date now = new Date();
                                                if (heureFinTime.after(now)) {
                                                    continue;
                                                }

                                                for (Map.Entry<String, SurveillantSanction> entry : sanctionsMap.entrySet()) {
                                                    SurveillantSanction sanction = entry.getValue();
                                                    Long surveillantId = Long.parseLong(entry.getKey().split("_")[0]);

                                                    // Comparer la salle du surveillant et la salle de l'examen
                                                    String surveillantRoom = surveillantRoomById.get(surveillantId);
                                                    String examRoom = examDoc.has("numero_salle") && !examDoc.get("numero_salle").isJsonNull()
                                                            ? examDoc.get("numero_salle").getAsString() : null;

                                                    boolean roomsMatch = (surveillantRoom != null && examRoom != null && surveillantRoom.equals(examRoom));

                                                    boolean hasPointage = false;
                                                    if (roomsMatch && pointagesByDayAndSession.containsKey(examDay.getTimeInMillis())) {
                                                        Map<String, List<Pointage>> sessionMap = pointagesByDayAndSession.get(examDay.getTimeInMillis());
                                                        if (sessionMap.containsKey(examSession)) {
                                                            for (Pointage p : sessionMap.get(examSession)) {
                                                                if (p.getId_surveillant().equals(surveillantId)) {
                                                                    hasPointage = true;

                                                                    // Vérifier retard: comparer heure_pointage avec heure_debut le même jour
                                                                    Calendar pCal = Calendar.getInstance();
                                                                    pCal.setTime(p.getHeure_pointage());

                                                                    Calendar debutCalSameDay = (Calendar) examDay.clone();
                                                                    debutCalSameDay.set(Calendar.HOUR_OF_DAY, heureDebCal.get(Calendar.HOUR_OF_DAY));
                                                                    debutCalSameDay.set(Calendar.MINUTE, heureDebCal.get(Calendar.MINUTE));
                                                                    debutCalSameDay.set(Calendar.SECOND, heureDebCal.get(Calendar.SECOND));

                                                                    if (pCal.after(debutCalSameDay)) {
                                                                        sanction.addRetard();
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Absence uniquement si la salle correspond et aucun pointage
                                                    if (roomsMatch && !hasPointage) {
                                                        sanction.addAbsence();
                                                    }
                                                }
                                            }

                                            // 5️⃣ Inclure tous les surveillants, même s'ils ont 0 retard et 0 absence
                                            List<SurveillantSanction> sanctionsList = new ArrayList<>(sanctionsMap.values());
                                            sanctionLiveData.setValue(sanctionsList);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        error.printStackTrace();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            error.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
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

    public LiveData<List<SurveillantSanction>> getSanctions() {
        return sanctionLiveData;
    }
}
