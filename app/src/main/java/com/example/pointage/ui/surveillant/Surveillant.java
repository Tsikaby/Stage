package com.example.pointage.ui.surveillant;

public class Surveillant {
    private int id_surveillant;
    private String numero_salle;
    private String nom_surveillant;
    private String contact;
    private String email;

    public Surveillant() { }

    public Surveillant(int id_surveillant, String numero_salle, String nom_surveillant, String contact, String email) {
        this.id_surveillant = id_surveillant;
        this.numero_salle = numero_salle;
        this.nom_surveillant = nom_surveillant;
        this.contact = contact;
        this.email = email;
    }

    // Getters et Setters
    public int getId_surveillant() { return id_surveillant; }
    public void setId_surveillant(int id_surveillant) { this.id_surveillant = id_surveillant; }

    public String getNumero_salle() { return numero_salle; }
    public void setNumero_salle(String numero_salle) { this.numero_salle = numero_salle; }

    public String getNom_surveillant() { return nom_surveillant; }
    public void setNom_surveillant(String nom_surveillant) { this.nom_surveillant = nom_surveillant; }

    public String getContact() { return contact; }

    public void setContact(String contact) { this.contact = contact; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

}
