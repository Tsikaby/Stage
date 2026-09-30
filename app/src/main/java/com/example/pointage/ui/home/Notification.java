package com.example.pointage.ui.home;

import java.util.Date;

public class Notification {
    private Long id;
    private Long idSurveillant; // ID du surveillant (pour les clés uniques)
    private String nomSurveillant;
    private String type; // "retard", "absence" ou "presence"
    private Date dateHeure;
    private String numeroSalle;
    private String session; // "Matin" ou "Après-midi" pour les absences
    private boolean lu;

    public Notification() {}

    public Notification(Long id, String nomSurveillant, String type, Date dateHeure, String numeroSalle) {
        this.id = id;
        this.idSurveillant = id; // Par défaut, utiliser id comme idSurveillant
        this.nomSurveillant = nomSurveillant;
        this.type = type;
        this.dateHeure = dateHeure;
        this.numeroSalle = numeroSalle;
        this.session = null;
        this.lu = false;
    }

    public Notification(Long id, String nomSurveillant, String type, Date dateHeure, String numeroSalle, String session) {
        this.id = id;
        this.idSurveillant = id; // Par défaut, utiliser id comme idSurveillant
        this.nomSurveillant = nomSurveillant;
        this.type = type;
        this.dateHeure = dateHeure;
        this.numeroSalle = numeroSalle;
        this.session = session;
        this.lu = false;
    }
/*
    public Notification(Long id, Long idSurveillant, String nomSurveillant, String type, Date dateHeure, String numeroSalle, String session) {
        this.id = id;
        this.idSurveillant = idSurveillant;
        this.nomSurveillant = nomSurveillant;
        this.type = type;
        this.dateHeure = dateHeure;
        this.numeroSalle = numeroSalle;
        this.session = session;
        this.lu = false;
    }
*/
    // Getters
    public Long getId() { return id; }
    public Long getIdSurveillant() { return idSurveillant; }
    public String getNomSurveillant() { return nomSurveillant; }
    public String getType() { return type; }
    public Date getDateHeure() { return dateHeure; }
    public String getNumeroSalle() { return numeroSalle; }
    public String getSession() { return session; }
    public boolean isLu() { return lu; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setIdSurveillant(Long idSurveillant) { this.idSurveillant = idSurveillant; }


    public boolean isRetard() {
        return "retard".equalsIgnoreCase(type);
    }

    public boolean isAbsence() {
        return "absence".equalsIgnoreCase(type);
    }
}