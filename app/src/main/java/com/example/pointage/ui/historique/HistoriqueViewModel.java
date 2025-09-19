package com.example.pointage.ui.historique;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.SupabaseClient;
import com.example.pointage.ui.historique.DateUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.URLEncoder;

public class HistoriqueViewModel extends ViewModel {

    private final MutableLiveData<List<Pointage>> historiqueLiveData = new MutableLiveData<>();
    private final SupabaseClient supabaseClient = SupabaseClient.getInstance();

    private int monthFilter = Calendar.getInstance().get(Calendar.MONTH);
    private int yearFilter = Calendar.getInstance().get(Calendar.YEAR);

    public HistoriqueViewModel() throws UnknownHostException {
        loadHistorique();
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
        String filter = "order=heure_pointage.desc";

        supabaseClient.select("pointage", "*", filter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
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

                        p.setId_surveillant(doc.get("id_surveillant").getAsLong());
                        p.setRetard(doc.get("retard").getAsBoolean());

                        if (doc.has("id_examen") && !doc.get("id_examen").isJsonNull()) {
                            p.setId_examen(doc.get("id_examen").getAsLong());
                        }

                        // Ces champs seront remplis par fetchSurveillantDataAndBind
                        p.setNom_surveillant("Chargement...");
                        p.setNumero_salle("Chargement...");
                        allPointages.add(p);
                    }
                    fetchSurveillantDataAndBind(allPointages);
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

    public void performScan(String numeroSalleSurveillant, Long idSurveillant, String nomSurveillant, final OnScanResultListener listener) {
        Date now = new Date();
        String session = DateUtils.getSession();
        String currentDate = DateUtils.getCurrentDateString();

        // Vérifier d'abord si le surveillant est assigné à cette salle
        String surveillantFilter = "id_surveillant=eq." + idSurveillant + "&numero_salle=eq." + numeroSalleSurveillant;

        supabaseClient.select("surveillant", "*", surveillantFilter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray surveillantResult) {
                if (surveillantResult.size() == 0) {
                    if (listener != null) listener.onScanFailure("Ce surveillant n'est pas assigné à cette salle.");
                    return;
                }

                // Maintenant chercher l'examen correspondant
                String examenFilter = "session=eq." + urlEncode(session) +
                        "&numero_salle=eq." + urlEncode(numeroSalleSurveillant) +
                        "&date_examen=eq." + urlEncode(currentDate);

                supabaseClient.select("examen", "*", examenFilter, new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(JsonArray examenResult) {
                        if (examenResult.size() == 0) {
                            if (listener != null) listener.onScanFailure("Aucun examen trouvé pour aujourd'hui dans cette salle (" + session + ")");
                            return;
                        }

                        try {
                            // Choisir le bon examen: si plusieurs (ex: 14h et 16h),
                            // prendre celui dont l'heure_debut est la plus proche mais <= maintenant.
                            // S'il n'y en a aucun avant, prendre le plus tôt de la session.
                            Calendar nowCal = Calendar.getInstance();
                            nowCal.setTime(now);
                            long nowMs = nowCal.getTimeInMillis();

                            JsonObject chosenExam = null;
                            long bestPastStart = Long.MIN_VALUE;
                            long bestFutureStart = Long.MAX_VALUE;
                            JsonObject earliestFutureExam = null;
                            long maxEndMs = Long.MIN_VALUE;

                            for (int i = 0; i < examenResult.size(); i++) {
                                JsonObject exam = examenResult.get(i).getAsJsonObject();
                                String heureDebutStr = exam.get("heure_debut").getAsString();
                                Date heureDebut = DateUtils.parseTime(heureDebutStr);

                                Calendar debutCal = Calendar.getInstance();
                                debutCal.setTime(heureDebut);
                                // Aligner la date sur aujourd'hui pour comparer correctement
                                debutCal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));
                                long startMs = debutCal.getTimeInMillis();

                                // Calculer heure de fin (si absente, fallback = heure_debut)
                                Date heureFin = null;
                                if (exam.has("heure_fin") && !exam.get("heure_fin").isJsonNull()) {
                                    try { heureFin = DateUtils.parseTime(exam.get("heure_fin").getAsString()); } catch (Exception ignored) {}
                                }
                                if (heureFin == null) heureFin = heureDebut;
                                Calendar finCal = Calendar.getInstance();
                                finCal.setTime(heureFin);
                                finCal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));
                                long endMs = finCal.getTimeInMillis();
                                if (endMs > maxEndMs) maxEndMs = endMs;

                                if (startMs <= nowMs) {
                                    if (startMs > bestPastStart) {
                                        bestPastStart = startMs;
                                        chosenExam = exam;
                                    }
                                } else {
                                    if (startMs < bestFutureStart) {
                                        bestFutureStart = startMs;
                                        earliestFutureExam = exam;
                                    }
                                }
                            }

                            if (chosenExam == null) {
                                // Aucun examen avant maintenant: prendre le plus tôt à venir
                                chosenExam = earliestFutureExam != null ? earliestFutureExam : examenResult.get(0).getAsJsonObject();
                            }

                            Long examenId = chosenExam.get("id_examen").getAsLong();
                            String heureDebutStr = chosenExam.get("heure_debut").getAsString();
                            Date heureDebut = DateUtils.parseTime(heureDebutStr);

                            // Calcul des bornes de l'examen choisi
                            Calendar debutCal = Calendar.getInstance();
                            debutCal.setTime(heureDebut);
                            debutCal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));
                            long startMsChosen = debutCal.getTimeInMillis();
                            // heure_fin du chosen (si absente, fallback = heure_debut)
                            Date heureFinChosen = null;
                            if (chosenExam.has("heure_fin") && !chosenExam.get("heure_fin").isJsonNull()) {
                                try { heureFinChosen = DateUtils.parseTime(chosenExam.get("heure_fin").getAsString()); } catch (Exception ignored) {}
                            }
                            if (heureFinChosen == null) heureFinChosen = heureDebut;
                            Calendar finCalChosen = Calendar.getInstance();
                            finCalChosen.setTime(heureFinChosen);
                            finCalChosen.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));
                            long endMsChosen = finCalChosen.getTimeInMillis();

                            // Règle demandée: si scan après la fin DU DERNIER EXAMEN de la session/salle du jour → ABSENT (pas de pointage)
                            boolean afterAllExams = nowMs > maxEndMs;
                            if (afterAllExams) {
                                if (listener != null) listener.onScanFailure("Pointage refusé: vous êtes considéré absent pour " + session + " (salle " + numeroSalleSurveillant + ").");
                                return;
                            }

                            // Sinon: retard si on scanne après le début de l'examen choisi
                            boolean isRetard = nowMs > startMsChosen;

                            String formattedDateTime = DateUtils.formatForSupabase(now);

                            JsonObject newPointageData = new JsonObject();
                            newPointageData.addProperty("id_pointage", System.currentTimeMillis()); // ensure unique id if schema requires it
                            newPointageData.addProperty("heure_pointage", formattedDateTime);
                            newPointageData.addProperty("id_surveillant", idSurveillant);
                            newPointageData.addProperty("retard", isRetard);
                            if (examenId != null) newPointageData.addProperty("id_examen", examenId);
                            supabaseClient.insert("pointage", newPointageData, new SupabaseClient.SupabaseCallback() {
                                @Override
                                public void onSuccess(JsonArray insertResult) {
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

                        } catch (ParseException e) {
                            if (listener != null) listener.onScanFailure("Erreur de format d'heure: " + e.getMessage());
                        } catch (Exception e) {
                            if (listener != null) listener.onScanFailure("Erreur: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        if (listener != null) listener.onScanFailure("Échec de la recherche d'examen: " + error.getMessage());
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

    private void fetchSurveillantDataAndBind(List<Pointage> pointageList) {
        List<Long> surveillantIds = new ArrayList<>();
        for (Pointage p : pointageList) {
            if (p.getId_surveillant() != null) {
                surveillantIds.add(p.getId_surveillant());
            }
        }

        if (surveillantIds.isEmpty()) {
            historiqueLiveData.setValue(pointageList);
            return;
        }

        // Interroger la table surveillant
        String surveillantIdsStr = String.join(",", surveillantIds.stream().map(String::valueOf).toArray(String[]::new));
        String filter = "id_surveillant=in.(" + surveillantIdsStr + ")";

        supabaseClient.select("surveillant", "*", filter, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                Map<Long, String> surveillantNameMap = new HashMap<>();
                Map<Long, String> surveillantRoomMap = new HashMap<>();

                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject doc = result.get(i).getAsJsonObject();
                        Long id = doc.get("id_surveillant").getAsLong();
                        surveillantNameMap.put(id, doc.get("nom_surveillant").getAsString());
                        surveillantRoomMap.put(id, doc.get("numero_salle").getAsString());
                    }

                    // Associer les données aux pointages
                    for (Pointage p : pointageList) {
                        if (p.getId_surveillant() != null) {
                            String nom = surveillantNameMap.get(p.getId_surveillant());
                            String numeroSalle = surveillantRoomMap.get(p.getId_surveillant());

                            p.setNom_surveillant(nom != null ? nom : "Surveillant inconnu");
                            p.setNumero_salle(numeroSalle != null ? numeroSalle : "Non spécifiée");
                        } else {
                            p.setNom_surveillant("Surveillant inconnu");
                            p.setNumero_salle("Non spécifiée");
                        }
                    }

                    historiqueLiveData.setValue(pointageList);
                } catch (Exception e) {
                    e.printStackTrace();
                    historiqueLiveData.setValue(pointageList);
                }
            }

            @Override
            public void onError(Exception error) {
                historiqueLiveData.setValue(pointageList);
            }
        });
    }

    private String urlEncode(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), "UTF-8");
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public void deletePointage(String documentId, OnDeleteListener listener) {
        String filter = "id_pointage=eq." + documentId;

        supabaseClient.delete("pointage", filter, new SupabaseClient.SupabaseCallback() {
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

    public LiveData<List<Pointage>> getHistorique() {
        return historiqueLiveData;
    }
}