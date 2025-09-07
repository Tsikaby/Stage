package com.example.pointage.ui.surveillant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SurveillantViewModel extends ViewModel {

    private final MutableLiveData<List<Surveillant>> surveillantsLiveData = new MutableLiveData<>();

    public SurveillantViewModel() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ecoute en temps réel la collection "surveillants"
        db.collection("surveillant")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<Surveillant> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Surveillant s = doc.toObject(Surveillant.class);
                            if (s != null) list.add(s);
                        }
                        surveillantsLiveData.setValue(list);
                    }
                });
    }

    public LiveData<List<Surveillant>> getSurveillants() {
        return surveillantsLiveData;
    }
}
