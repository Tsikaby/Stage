package com.example.pointage.ui.sanction;

public class SurveillantSanction {
    private String nom_surveillant;
    private int nombre_retards = 0;
    private int nombre_absences = 0;
    private int mois;
    private int annee;

    public SurveillantSanction(String nom, int mois, int annee) {
        this.nom_surveillant = nom;
        this.mois = mois;
        this.annee = annee;
    }
    public void addRetard() { nombre_retards++; }
    public void addAbsence() { nombre_absences++; }

    public void reset() {
        nombre_retards = 0;
        nombre_absences = 0;
    }

    public void resetAbsences() {
        nombre_absences = 0;
    }

    public String getNom_surveillant() { return nom_surveillant; }
    public int getNombre_retards() { return nombre_retards; }
    public int getNombre_absences() { return nombre_absences; }
    public int getMois() { return mois; }
    public int getAnnee() { return annee; }
}