package com.example.pointage.ui.surveillant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SurveillantViewModel extends ViewModel {

    private final MutableLiveData<List<Surveillant>> surveillantsLiveData = new MutableLiveData<>();

    public SurveillantViewModel() throws UnknownHostException {
        SupabaseClient supabaseClient = SupabaseClient.getInstance();

        // Récupérer la collection "surveillant"
        supabaseClient.select("surveillant", "*", null, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                List<Surveillant> list = new ArrayList<>();
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject doc = result.get(i).getAsJsonObject();
                        Surveillant s = new Surveillant();
                        s.setId_surveillant(doc.get("id_surveillant").getAsInt());
                        s.setNom_surveillant(doc.get("nom_surveillant").getAsString());
                        s.setNumero_salle(doc.get("numero_salle").getAsString());
                        s.setContact(doc.get("contact").getAsString()); // Utiliser groupe_surveillant comme contact
                        list.add(s);
                    }
                    surveillantsLiveData.setValue(list);
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

    public LiveData<List<Surveillant>> getSurveillants() {
        return surveillantsLiveData;
    }
}
