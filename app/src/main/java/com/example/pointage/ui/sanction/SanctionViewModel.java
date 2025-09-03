package com.example.pointage.ui.sanction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SanctionViewModel extends ViewModel {

    private static MutableLiveData<String> mText;

    public SanctionViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Liste des des surveillants menacés d'une ou de plusieurs sanction(s)");
    }

    public static LiveData<String> getText() {
        return mText;
    }
}