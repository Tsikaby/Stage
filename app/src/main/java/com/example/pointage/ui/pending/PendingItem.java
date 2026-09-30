package com.example.pointage.ui.pending;

public class PendingItem {
    private final long idSurveillant;
    private final String nom;
    private final String numeroSalle;
    private final String deadline;
    private final String session; // "Matin" ou "Après-midi"

    public PendingItem(long idSurveillant, String nom, String numeroSalle, String deadline, String session) {
        this.idSurveillant = idSurveillant;
        this.nom = nom;
        this.numeroSalle = numeroSalle;
        this.deadline = deadline;
        this.session = session;
    }

    public long getIdSurveillant() { return idSurveillant; }
    public String getNom() { return nom; }
    public String getNumeroSalle() { return numeroSalle; }
    public String getDeadline() { return deadline; }
    public String getSession() { return session; }
}
