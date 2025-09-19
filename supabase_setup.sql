-- Script de création des tables pour Supabase
-- Exécuter ce script dans l'éditeur SQL de Supabase

-- Table des utilisateurs
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    mdp VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table des surveillants
CREATE TABLE surveillant (
    id SERIAL PRIMARY KEY,
    id_surveillant BIGINT UNIQUE NOT NULL,
    nom_surveillant VARCHAR(255) NOT NULL,
    numero_salle VARCHAR(50),
    contact VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table des examens
CREATE TABLE examen (
    id SERIAL PRIMARY KEY,
    session VARCHAR(50) NOT NULL,
    numero_salle VARCHAR(50) NOT NULL,
    heure_debut TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table des pointages
CREATE TABLE pointage (
    id SERIAL PRIMARY KEY,
    id_pointage BIGINT UNIQUE NOT NULL,
    heure_pointage TIMESTAMP NOT NULL,
    id_surveillant BIGINT NOT NULL,
    nom_surveillant VARCHAR(255) NOT NULL,
    contact VARCHAR(255),
    numero_salle VARCHAR(50) NOT NULL,
    retard BOOLEAN DEFAULT FALSE,
    id_examen INTEGER REFERENCES examen(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Activer RLS (Row Level Security) sur toutes les tables
ALTER TABLE utilisateurs ENABLE ROW LEVEL SECURITY;
ALTER TABLE surveillant ENABLE ROW LEVEL SECURITY;
ALTER TABLE examen ENABLE ROW LEVEL SECURITY;
ALTER TABLE pointage ENABLE ROW LEVEL SECURITY;

-- Politiques pour permettre la lecture/écriture (à adapter selon vos besoins)
CREATE POLICY "Allow all operations on utilisateurs" ON utilisateurs FOR ALL USING (true);
CREATE POLICY "Allow all operations on surveillant" ON surveillant FOR ALL USING (true);
CREATE POLICY "Allow all operations on examen" ON examen FOR ALL USING (true);
CREATE POLICY "Allow all operations on pointage" ON pointage FOR ALL USING (true);

-- Insérer quelques données de test
INSERT INTO utilisateurs (username, mdp) VALUES 
('admin', 'admin123'),
('user1', 'password123');

INSERT INTO surveillant (id_surveillant, nom_surveillant, numero_salle, contact) VALUES 
(1, 'Jean Dupont', 'A101', 'jean.dupont@email.com'),
(2, 'Marie Martin', 'A102', 'marie.martin@email.com'),
(3, 'Pierre Durand', 'B201', 'pierre.durand@email.com');

INSERT INTO examen (session, numero_salle, heure_debut) VALUES 
('Matin', 'A101', '2024-01-15 08:00:00'),
('Après-midi', 'A102', '2024-01-15 14:00:00'),
('Matin', 'B201', '2024-01-16 08:30:00');

