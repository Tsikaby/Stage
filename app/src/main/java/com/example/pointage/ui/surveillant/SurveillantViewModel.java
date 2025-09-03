package com.example.pointage.ui.surveillant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SurveillantViewModel extends ViewModel {

    private static MutableLiveData<String> mText;

    public SurveillantViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Liste des surveillants ");
    }

    public static LiveData<String> getText() {
        return mText;
    }
}