# Guide de dépannage - Application Pointage avec Supabase

## Problème : Rien ne s'affiche dans l'application

### 1. Vérifier la configuration Supabase

**Étape 1 : Exécuter le script SQL**
```sql
-- Exécuter ce script dans l'éditeur SQL de Supabase
-- (contenu du fichier supabase_setup_updated.sql)
```

**Étape 2 : Vérifier les clés Supabase**
Dans `SupabaseClient.java`, vérifiez que vos clés sont correctes :
```java
private static final String SUPABASE_URL = "https://sidshqdnmtccxgfzzrve.supabase.co";
private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
```

### 2. Vérifier les données dans Supabase

**Vérifier la table surveillant :**
```sql
SELECT * FROM surveillant;
```

**Vérifier la table pointage :**
```sql
SELECT * FROM pointage;
```

**Vérifier la table examen :**
```sql
SELECT * FROM examen;
```

### 3. Tester les requêtes API

**Test de la requête surveillant :**
```bash
curl -X GET "https://sidshqdnmtccxgfzzrve.supabase.co/rest/v1/surveillant" \
  -H "apikey: VOTRE_CLE_ANONYME" \
  -H "Authorization: Bearer VOTRE_CLE_ANONYME"
```

**Test de la requête pointage :**
```bash
curl -X GET "https://sidshqdnmtccxgfzzrve.supabase.co/rest/v1/pointage" \
  -H "apikey: VOTRE_CLE_ANONYME" \
  -H "Authorization: Bearer VOTRE_CLE_ANONYME"
```

### 4. Vérifier les logs Android

**Activer les logs :**
1. Ouvrir Android Studio
2. Aller dans View > Tool Windows > Logcat
3. Filtrer par "POINTAGE" ou "SupabaseClient"
4. Lancer l'application et regarder les logs

### 5. Problèmes courants et solutions

**Problème : "No data found"**
- Vérifier que les tables contiennent des données
- Vérifier les politiques RLS (Row Level Security)
- Vérifier que les clés API sont correctes

**Problème : "Network error"**
- Vérifier la connexion internet
- Vérifier l'URL Supabase
- Vérifier les permissions internet dans AndroidManifest.xml

**Problème : "Authentication error"**
- Vérifier la clé anonyme Supabase
- Vérifier les politiques RLS

### 6. Structure des données attendue

**Table surveillant :**
```sql
id_surveillant (integer) | nom_surveillant (varchar) | groupe_surveillant (varchar) | numero_salle (varchar)
```

**Table pointage :**
```sql
id_pointage (bigint) | id_surveillant (bigint) | heure_pointage (timestamp) | retard (boolean) | date_pointage (date)
```

**Table examen :**
```sql
id_examen (bigint) | date_examen (date) | heure_debut (timestamp) | numero_salle (varchar) | session (varchar)
```

### 7. Données de test

**Insérer des surveillants de test :**
```sql
INSERT INTO surveillant (id_surveillant, nom_surveillant, groupe_surveillant, numero_salle) VALUES 
(1, 'Jean Dupont', 'Groupe A', 'A101'),
(2, 'Marie Martin', 'Groupe B', 'A102');
```

**Insérer des pointages de test :**
```sql
INSERT INTO pointage (id_surveillant, heure_pointage, retard, date_pointage) VALUES 
(1, NOW(), false, CURRENT_DATE),
(2, NOW(), true, CURRENT_DATE);
```

### 8. Debugging avancé

**Ajouter des logs dans SupabaseClient.java :**
```java
Log.d("SupabaseClient", "URL: " + url);
Log.d("SupabaseClient", "Response: " + responseBody);
```

**Vérifier les erreurs HTTP :**
```java
Log.e("SupabaseClient", "HTTP Error: " + response.code() + " - " + response.message());
```

### 9. Contact support

Si le problème persiste :
1. Vérifier les logs Android Studio
2. Vérifier les logs Supabase (Dashboard > Logs)
3. Tester les requêtes API directement
4. Vérifier la configuration RLS

