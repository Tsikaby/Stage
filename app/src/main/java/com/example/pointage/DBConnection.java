package com.example.pointage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // ⚠️ Mets ici tes infos Neon
    private static final String URL = "postgresql://neondb_owner:npg_0k9ZLeWfixtB@ep-jolly-sunset-adyqce1f-pooler.c-2.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_0k9ZLeWfixtB";

    private static Connection connection = null;

    // Méthode pour obtenir la connexion
    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion réussie à Neon PostgreSQL !");
            } catch (SQLException e) {
                System.out.println("❌ Erreur connexion : " + e.getMessage());
            }
        }
        return connection;
    }

    // Méthode pour fermer la connexion
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔒 Connexion fermée.");
            } catch (SQLException e) {
                System.out.println("❌ Erreur fermeture connexion : " + e.getMessage());
            }
        }
    }
}
