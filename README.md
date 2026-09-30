Il faut installer

Node.js** (v18 ou supérieur) avec npm
PostgreSQL** (v12 ou supérieur)
Android Studio** (pour l'application Android)
Git

Configuration du Backend

Installer les dépendances

```bash
cd backend
npm install
```

Configurer la base de données PostgreSQL

```bash
# Créer la base de données
psql -U postgres
CREATE DATABASE pointage_db;
\q

# Exécuter le script SQL
psql -U postgres -d pointage_db -f ../pointage_db.sql
```

 Créer le fichier .env

Copiez le fichier d'exemple et modifiez-le avec vos configurations:

```bash
cd backend
cp .env.example .env
```

Éditez le fichier `.env` avec vos valeurs:

```env
PORT=8080
PGHOST=localhost
PGPORT=5432
PGUSER=postgres
PGPASSWORD=votre_mot_de_passe
PGDATABASE=pointage_db
```


 Démarrer le serveur backend

```bash
cd backend
npm start
```



 Configurer l'URL API

L'application Android doit être configurée pour se connecter à votre serveur backend.

Pour un développement local (émulateur Android):**
- L'URL API par défaut est `http://10.0.2.2:8080` (adresse spéciale de l'émulateur qui pointe vers localhost)

Pour un appareil physique sur le même réseau WiFi:**
1. Trouvez votre adresse IPv4 sur votre réseau WiFi:
   -Windows: `ipconfig` (cherchez "Adresse IPv4")
 
   
2. Modifiez l'URL API dans le fichier:
   ```
   app/src/main/java/com/example/pointage/ConnectClient.java
   ```
   
3. Remplacez l'URL par votre adresse IPv4:
   ```java
   private static final String BASE_URL = "http://VOTRE_ADRESSE_IPV4:8080/api";
   ```

Exemple:
```java
private static final String BASE_URL = "http://192.168.1.100:8080/api";

1. Lancez le backend: `cd backend && npm start`
2. Lancez l'application Android
3. Connectez-vous avec un compte utilisateur (créé dans la base de données)

Scan QR Code

1. Cliquez sur le bouton "+" pour scanner un QR code
2. Alignez le QR code du surveillant
3. Le pointage sera enregistré automatiquement
