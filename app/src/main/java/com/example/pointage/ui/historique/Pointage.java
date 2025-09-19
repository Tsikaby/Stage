package com.example.pointage.ui.historique;

import java.util.Date;

public class Pointage {
    private Long id_pointage;
    private Date heure_pointage;
    private Long id_surveillant;
    private Long id_examen;
    private boolean retard;
    private String nom_surveillant;
    private String id;
    private String numero_salle;

    public Pointage() {}

    public Long getId_pointage() { return id_pointage; }
    public Date getHeure_pointage() { return heure_pointage; }
    public Long getId_surveillant() { return id_surveillant; }
    public Long getId_examen() { return id_examen; }
    public boolean isRetard() { return retard; }
    public String getNom_surveillant() { return nom_surveillant; }
    public String getId() { return id; }
    public String getNumero_salle() { return numero_salle; }

    public void setId_pointage(Long id_pointage) { this.id_pointage = id_pointage; }
    public void setHeure_pointage(Date heure_pointage) { this.heure_pointage = heure_pointage; }
    public void setId_surveillant(Long id_surveillant) { this.id_surveillant = id_surveillant; }
    public void setId_examen(Long id_examen) { this.id_examen = id_examen; }
    public void setRetard(boolean retard) { this.retard = retard; }
    public void setNom_surveillant(String nom_surveillant) { this.nom_surveillant = nom_surveillant; }
    public void setId(String id) { this.id = id; }
    public void setNumero_salle(String numero_salle) { this.numero_salle = numero_salle; }
}