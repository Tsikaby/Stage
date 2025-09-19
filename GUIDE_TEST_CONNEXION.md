# 🔍 Guide de test de connexion Supabase

## Méthode 1: Test via Android Studio (Recommandé)

### Étape 1: Compiler et installer l'application
```bash
./gradlew assembleDebug
```

### Étape 2: Ouvrir les logs
1. Ouvrir Android Studio
2. Aller dans `View` > `Tool Windows` > `Logcat`
3. Filtrer par "TestSupabase" ou "SupabaseClient"

### Étape 3: Lancer l'application
- L'application va automatiquement tester la connexion au démarrage
- Regarder les logs pour voir les résultats

### Étape 4: Test manuel
- **Clic long sur le bouton FAB** (bouton rond) pour relancer le test
- Regarder les logs et les messages Snackbar

## Méthode 2: Test via ligne de commande

### Étape 1: Exécuter le script de test
```bash
# Double-cliquer sur test_supabase_connection.bat
# ou exécuter dans PowerShell:
.\test_supabase_connection.bat
```

### Étape 2: Vérifier les réponses
- ✅ **Code 200** = Connexion réussie
- ❌ **Code 401** = Problème d'authentification
- ❌ **Code 404** = Table non trouvée
- ❌ **Code 500** = Erreur serveur

## Méthode 3: Test via l'interface Supabase

### Étape 1: Aller sur le dashboard Supabase
1. Ouvrir [supabase.com](https://supabase.com)
2. Se connecter à votre compte
3. Ouvrir votre projet

### Étape 2: Vérifier les données
1. Aller dans `Table Editor`
2. Vérifier que les tables contiennent des données:
   - `surveillant`
   - `pointage`
   - `examen`

### Étape 3: Tester les requêtes SQL
```sql
-- Test 1: Vérifier les surveillants
SELECT * FROM surveillant LIMIT 5;

-- Test 2: Vérifier les pointages
SELECT * FROM pointage LIMIT 5;

-- Test 3: Vérifier les examens
SELECT * FROM examen LIMIT 5;
```

## Interprétation des résultats

### ✅ Connexion réussie
```
✅ Test 1 RÉUSSI: 3 surveillants trouvés
✅ Test 2 RÉUSSI: 5 pointages trouvés
✅ Test 3 RÉUSSI: Pointage de test inséré
🎉 TOUS LES TESTS SONT PASSÉS - CONNEXION SUPABASE OK!
```

### ❌ Problèmes courants

**Erreur 401 - Non autorisé:**
- Vérifier la clé API dans `SupabaseClient.java`
- Vérifier les politiques RLS dans Supabase

**Erreur 404 - Table non trouvée:**
- Vérifier que les tables existent dans Supabase
- Vérifier les noms des tables (minuscules/majuscules)

**Erreur de réseau:**
- Vérifier la connexion internet
- Vérifier l'URL Supabase

**Aucune donnée:**
- Insérer des données de test dans Supabase
- Vérifier les politiques RLS

## Données de test à insérer

```sql
-- Surveillants de test
INSERT INTO surveillant (id_surveillant, nom_surveillant, groupe_surveillant, numero_salle) VALUES 
(1, 'Jean Dupont', 'Groupe A', 'A101'),
(2, 'Marie Martin', 'Groupe B', 'A102'),
(3, 'Pierre Durand', 'Groupe A', 'B201');

-- Pointages de test
INSERT INTO pointage (id_surveillant, heure_pointage, retard, date_pointage) VALUES 
(1, NOW(), false, CURRENT_DATE),
(2, NOW(), true, CURRENT_DATE);

-- Examens de test
INSERT INTO examen (id_examen, date_examen, heure_debut, numero_salle, session) VALUES 
(1, CURRENT_DATE, NOW(), 'A101', 'Matin'),
(2, CURRENT_DATE, NOW() + INTERVAL '6 hours', 'A102', 'Après-midi');
```

## Résolution des problèmes

### Problème: "No data found"
1. Vérifier que les tables contiennent des données
2. Vérifier les politiques RLS
3. Exécuter les requêtes SQL de test

### Problème: "Network error"
1. Vérifier la connexion internet
2. Vérifier l'URL Supabase
3. Vérifier les permissions dans AndroidManifest.xml

### Problème: "Authentication error"
1. Vérifier la clé API
2. Vérifier les politiques RLS
3. Vérifier que l'utilisateur a les bonnes permissions

## Contact support

Si les tests échouent:
1. Copier les logs d'erreur
2. Vérifier la configuration Supabase
3. Tester les requêtes API directement
4. Vérifier les données dans Supabase

