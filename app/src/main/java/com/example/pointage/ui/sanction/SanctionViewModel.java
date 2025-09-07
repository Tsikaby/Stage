package com.example.pointage.ui.sanction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ui.historique.Pointage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SanctionViewModel extends ViewModel {

    private final MutableLiveData<List<SurveillantSanction>> sanctionLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public SanctionViewModel() {
        Calendar currentCalendar = Calendar.getInstance();
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        loadSanctions(currentMonth, currentYear);
    }

    public void loadSanctions(int month, int year) {
        Map<String, SurveillantSanction> sanctionsMap = new HashMap<>();

        // Fetch surveillants
        db.collection("surveillant").get().addOnSuccessListener(surveillantQuery -> {
            for (QueryDocumentSnapshot surveillantDoc : surveillantQuery) {
                Long surveillantId = surveillantDoc.getLong("id_surveillant");
                String nomSurveillant = surveillantDoc.getString("nom_surveillant");
                if (surveillantId != null && nomSurveillant != null) {
                    String key = surveillantId + "_" + month;
                    sanctionsMap.put(key, new SurveillantSanction(nomSurveillant, month));
                }
            }

            // Fetch exams and filter by month and year
            db.collection("examen").get().addOnSuccessListener(examQuery -> {
                List<Calendar> examDays = new ArrayList<>();
                for (QueryDocumentSnapshot examDoc : examQuery) {
                    Calendar examDay = getStartOfDay(examDoc.getTimestamp("date_examen").toDate());
                    if (examDay.get(Calendar.MONTH) == month && examDay.get(Calendar.YEAR) == year) {
                        examDays.add(examDay);
                    }
                }

                if (examDays.isEmpty()) {
                    sanctionLiveData.setValue(new ArrayList<>());
                    return;
                }

                // Fetch pointages and process
                db.collection("pointage").get().addOnSuccessListener(pointageQuery -> {
                    Map<Long, List<Pointage>> pointagesByDay = new HashMap<>();
                    for (QueryDocumentSnapshot pointageDoc : pointageQuery) {
                        Pointage pointage = pointageDoc.toObject(Pointage.class);
                        if (pointage.getHeure_pointage() == null) continue;

                        Calendar pointageDay = getStartOfDay(pointage.getHeure_pointage().toDate());
                        int pointageMonth = pointageDay.get(Calendar.MONTH);
                        int pointageYear = pointageDay.get(Calendar.YEAR);

                        if (pointageMonth != month || pointageYear != year) continue;

                        for (Calendar examDay : examDays) {
                            if (pointageDay.equals(examDay)) {
                                if (pointage.isRetard()) {
                                    String key = pointage.getId_surveillant() + "_" + month;
                                    SurveillantSanction sanction = sanctionsMap.get(key);
                                    if (sanction != null) {
                                        sanction.addRetard();
                                    }
                                }
                                List<Pointage> listForDay = pointagesByDay.getOrDefault(examDay.getTimeInMillis(), new ArrayList<>());
                                listForDay.add(pointage);
                                pointagesByDay.put(examDay.getTimeInMillis(), listForDay);
                                break;
                            }
                        }
                    }

                    // Logic for absences
                    for (Map.Entry<String, SurveillantSanction> entry : sanctionsMap.entrySet()) {
                        SurveillantSanction sanction = entry.getValue();
                        Long surveillantId = Long.parseLong(entry.getKey().split("_")[0]);

                        for (Calendar examDay : examDays) {
                            boolean hasPointage = false;
                            if (pointagesByDay.containsKey(examDay.getTimeInMillis())) {
                                for (Pointage pointage : pointagesByDay.get(examDay.getTimeInMillis())) {
                                    if (pointage.getId_surveillant().equals(surveillantId)) {
                                        hasPointage = true;
                                        break;
                                    }
                                }
                            }
                            if (!hasPointage) {
                                sanction.addAbsence();
                            }
                        }
                    }

                    // Filter surveillants with sanctions
                    List<SurveillantSanction> sanctionsList = new ArrayList<>();
                    for (SurveillantSanction s : sanctionsMap.values()) {
                        if (s.getNombre_retards() > 0 || s.getNombre_absences() > 0) {
                            sanctionsList.add(s);
                        }
                    }
                    sanctionLiveData.setValue(sanctionsList);
                }).addOnFailureListener(e -> {
                    // handle error
                });
            }).addOnFailureListener(e -> {
                // handle error
            });
        }).addOnFailureListener(e -> {
            // handle error
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