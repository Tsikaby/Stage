# Migration de Firebase vers Supabase

## Résumé des changements

Cette migration remplace Firebase Firestore par Supabase pour la gestion de la base de données dans l'application de pointage.

## Changements effectués

### 1. Dépendances Gradle
- **Supprimé** : Toutes les dépendances Firebase (firebase-analytics, firebase-bom, firebase-firestore, firebase-auth)
- **Ajouté** : Dépendances Supabase (supabase-postgrest, supabase-realtime, supabase-storage, supabase-auth)
- **Ajouté** : OkHttp pour les appels HTTP

### 2. Configuration
- **Supprimé** : Plugin Google Services (`com.google.gms.google-services`)
- **Supprimé** : Toutes les références Firebase dans les fichiers de configuration

### 3. Code Java
- **Créé** : `SupabaseClient.java` - Classe utilitaire pour interagir avec l'API Supabase
- **Modifié** : `Pointage.java` - Remplacé `Timestamp` par `Date` pour la compatibilité Supabase
- **Migré** : Tous les ViewModels et Activities pour utiliser Supabase au lieu de Firebase

### 4. Fonctionnalités migrées
- ✅ Authentification des utilisateurs
- ✅ Gestion des pointages
- ✅ Historique des pointages
- ✅ Gestion des surveillants
- ✅ Calcul des sanctions

## Configuration requise

### 1. Créer un projet Supabase
1. Aller sur [supabase.com](https://supabase.com)
2. Créer un nouveau projet
3. Récupérer l'URL et la clé anonyme

### 2. Configurer les clés Supabase
Dans `SupabaseClient.java`, remplacer :
```java
private static final String SUPABASE_URL = "YOUR_SUPABASE_URL";
private static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";
```

### 3. Créer les tables dans Supabase
Exécuter les requêtes SQL suivantes dans l'éditeur SQL de Supabase :

```sql
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
```

### 4. Configurer les politiques RLS (Row Level Security)
```sql
-- Activer RLS sur toutes les tables
ALTER TABLE utilisateurs ENABLE ROW LEVEL SECURITY;
ALTER TABLE surveillant ENABLE ROW LEVEL SECURITY;
ALTER TABLE examen ENABLE ROW LEVEL SECURITY;
ALTER TABLE pointage ENABLE ROW LEVEL SECURITY;

-- Politiques pour permettre la lecture/écriture (à adapter selon vos besoins)
CREATE POLICY "Allow all operations" ON utilisateurs FOR ALL USING (true);
CREATE POLICY "Allow all operations" ON surveillant FOR ALL USING (true);
CREATE POLICY "Allow all operations" ON examen FOR ALL USING (true);
CREATE POLICY "Allow all operations" ON pointage FOR ALL USING (true);
```

## Avantages de la migration

1. **Open Source** : Supabase est open source contrairement à Firebase
2. **PostgreSQL** : Base de données relationnelle robuste
3. **API REST** : Interface simple et standardisée
4. **Coût** : Généralement plus économique que Firebase
5. **Flexibilité** : Plus de contrôle sur la base de données

## Notes importantes

- Les types de données ont été adaptés pour Supabase (Date au lieu de Timestamp)
- Les requêtes utilisent maintenant la syntaxe PostgREST de Supabase
- L'authentification simple par nom d'utilisateur/mot de passe est maintenue
- Toutes les fonctionnalités existantes sont préservées

## Test de la migration

1. Configurer les clés Supabase
2. Créer les tables dans Supabase
3. Compiler et installer l'application
4. Tester toutes les fonctionnalités (connexion, pointage, historique, etc.)

