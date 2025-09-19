-- Script de configuration pour votre schéma Supabase existant
-- Exécuter ce script dans l'éditeur SQL de Supabase

-- Activer RLS (Row Level Security) sur toutes les tables
ALTER TABLE examen ENABLE ROW LEVEL SECURITY;
ALTER TABLE surveillant ENABLE ROW LEVEL SECURITY;
ALTER TABLE salle ENABLE ROW LEVEL SECURITY;
ALTER TABLE pointage ENABLE ROW LEVEL SECURITY;

-- Politiques pour permettre la lecture/écriture (à adapter selon vos besoins)
CREATE POLICY "Allow all operations on examen" ON examen FOR ALL USING (true);
CREATE POLICY "Allow all operations on surveillant" ON surveillant FOR ALL USING (true);
CREATE POLICY "Allow all operations on salle" ON salle FOR ALL USING (true);
CREATE POLICY "Allow all operations on pointage" ON pointage FOR ALL USING (true);

-- Insérer quelques données de test pour les surveillants
INSERT INTO surveillant (id_surveillant, nom_surveillant, groupe_surveillant, numero_salle) VALUES 
(1, 'Jean Dupont', 'Groupe A', 'A101'),
(2, 'Marie Martin', 'Groupe B', 'A102'),
(3, 'Pierre Durand', 'Groupe A', 'B201')
ON CONFLICT (id_surveillant) DO NOTHING;

-- Insérer quelques données de test pour les salles
INSERT INTO salle (numero_salle, capacite_max, nbr_surveillant) VALUES 
('A101', 30, 2),
('A102', 25, 2),
('B201', 35, 3)
ON CONFLICT (numero_salle) DO NOTHING;

-- Insérer quelques données de test pour les examens
INSERT INTO examen (id_examen, date_examen, heure_debut, heure_fin, duree, numero_salle, session) VALUES 
(1, '2024-01-15', '2024-01-15 08:00:00', '2024-01-15 10:00:00', 120.0, 'A101', 'Matin'),
(2, '2024-01-15', '2024-01-15 14:00:00', '2024-01-15 16:00:00', 120.0, 'A102', 'Après-midi'),
(3, '2024-01-16', '2024-01-16 08:30:00', '2024-01-16 10:30:00', 120.0, 'B201', 'Matin')
ON CONFLICT (id_examen) DO NOTHING;

-- Créer une table utilisateurs si elle n'existe pas
CREATE TABLE IF NOT EXISTS utilisateurs (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    mdp VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Activer RLS sur la table utilisateurs
ALTER TABLE utilisateurs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow all operations on utilisateurs" ON utilisateurs FOR ALL USING (true);

-- Insérer des utilisateurs de test
INSERT INTO utilisateurs (username, mdp) VALUES 
('admin', 'admin123'),
('user1', 'password123')
ON CONFLICT (username) DO NOTHING;

