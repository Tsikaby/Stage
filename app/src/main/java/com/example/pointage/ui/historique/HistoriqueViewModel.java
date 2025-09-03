package com.example.pointage.ui.historique;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoriqueViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HistoriqueViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Voici l'historique des scans");
    }

    public LiveData<String> getText() {
        return mText;
    }
}