package com.example.pointage.ui.historique;

import com.google.firebase.Timestamp;

public class Pointage {
    private Long id_pointage;
    private Timestamp heure_pointage;
    private Long id_surveillant;
    private boolean retard;
    private String nom_surveillant;
    private String id;

    public Pointage() {}

    public Long getId_pointage() { return id_pointage; }
    public Timestamp getHeure_pointage() { return heure_pointage; }
    public Long getId_surveillant() { return id_surveillant; }
    public boolean isRetard() { return retard; }
    public String getNom_surveillant() { return nom_surveillant; }
    public String getId() { return id; }


    public void setId(String id) { this.id = id; }
    public void setId_pointage(Long id_pointage) { this.id_pointage = id_pointage; }
    public void setHeure_pointage(Timestamp heure_pointage) { this.heure_pointage = heure_pointage; }
    public void setId_surveillant(Long id_surveillant) { this.id_surveillant = id_surveillant; }
    public void setRetard(boolean retard) { this.retard = retard; }
    public void setNom_surveillant(String nom_surveillant) { this.nom_surveillant = nom_surveillant; }
}