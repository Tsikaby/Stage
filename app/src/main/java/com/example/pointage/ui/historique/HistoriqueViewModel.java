package com.example.pointage.ui.historique;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoriqueViewModel extends ViewModel {

    private final MutableLiveData<List<Pointage>> historiqueLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private int monthFilter = Calendar.getInstance().get(Calendar.MONTH);
    private int yearFilter = Calendar.getInstance().get(Calendar.YEAR);

    public HistoriqueViewModel() {
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
        db.collection("pointage")
                .orderBy("heure_pointage", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<Pointage> allPointages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Pointage pointage = doc.toObject(Pointage.class);
                            pointage.setId(doc.getId());
                            allPointages.add(pointage);
                        }

                        List<Pointage> filtered = new ArrayList<>();
                        for (Pointage p : allPointages) {
                            if (p.getHeure_pointage() != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(p.getHeure_pointage().toDate());
                                if (cal.get(Calendar.MONTH) == monthFilter &&
                                        cal.get(Calendar.YEAR) == yearFilter) {
                                    filtered.add(p);
                                }
                            }
                        }

                        fetchSurveillantNames(filtered);
                    }
                });
    }

    private void fetchSurveillantNames(List<Pointage> pointageList) {
        db.collection("surveillant").get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<Long, String> surveillantMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long id = doc.getLong("id_surveillant");
                        String nom = doc.getString("nom_surveillant");
                        surveillantMap.put(id, nom);
                    }
                    for (Pointage p : pointageList) {
                        if (p.getId_surveillant() != null) {
                            p.setNom_surveillant(surveillantMap.get(p.getId_surveillant()));
                        }
                    }
                    historiqueLiveData.setValue(pointageList);
                })
                .addOnFailureListener(e -> historiqueLiveData.setValue(pointageList));
    }

    public void deletePointage(String documentId, OnDeleteListener listener) {
        db.collection("pointage").document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                    loadHistorique();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
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