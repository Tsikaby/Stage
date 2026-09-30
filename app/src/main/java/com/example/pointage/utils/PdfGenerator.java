package com.example.pointage.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.icu.text.DateFormatSymbols;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.pointage.ConnectClient;
import com.example.pointage.ui.historique.Pointage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfGenerator {

    public interface OnPdfGeneratedListener {
        void onPdfGenerated(Uri pdfUri, String nomSurveillant);
        void onError(String errorMessage);
    }

    private Context context;

    public PdfGenerator(Context context) {
        this.context = context;
    }

    public void generateSurveillantReport(String nomSurveillant, int idSurveillant, List<Pointage> allPointages, OnPdfGeneratedListener listener) {
        // Filtrer les pointages pour ce surveillant
        List<Pointage> surveillantPointages = new ArrayList<>();
        for (Pointage p : allPointages) {
            if (p.getId_surveillant() != null && p.getId_surveillant() == idSurveillant) {
                surveillantPointages.add(p);
            }
        }

        // Récupérer les absences depuis la base de données
        fetchAbsencesAndGeneratePdf(nomSurveillant, idSurveillant, surveillantPointages, listener);
    }

    private void fetchAbsencesAndGeneratePdf(String nomSurveillant, int idSurveillant, List<Pointage> surveillantPointages, OnPdfGeneratedListener listener) {
        try {
            ConnectClient connectClient = ConnectClient.getInstance();
            String filter = "id_surveillant=eq." + idSurveillant;

            connectClient.select("sanction", "*", filter, new ConnectClient.ClientCallback() {
                @Override
                public void onSuccess(JsonArray result) {
                    Map<String, Integer> absencesByMonth = new HashMap<>();
                    Map<String, Integer> retardsByMonth = new HashMap<>();
                    List<AbsenceDetail> absenceDetails = new ArrayList<>();
                    List<RetardDetail> retardDetails = new ArrayList<>();
                    
                    // Compter les absences et retards par mois et collecter les détails
                    for (int i = 0; i < result.size(); i++) {
                        try {
                            JsonObject sanction = result.get(i).getAsJsonObject();
                            if (sanction.has("date_examen") && !sanction.get("date_examen").isJsonNull()) {
                                String dateStr = sanction.get("date_examen").getAsString();
                                String heureStr = sanction.has("session") && !sanction.get("session").isJsonNull() ?
                                    sanction.get("session").getAsString() : "N/A";
                                String salle = sanction.has("numero_salle") && !sanction.get("numero_salle").isJsonNull() ? 
                                    sanction.get("numero_salle").getAsString() : "N/A";
                                String type = sanction.has("type") && !sanction.get("type").isJsonNull() ?
                                    sanction.get("type").getAsString() : "";
                                
                                // Parser la date pour extraire le mois
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = inputFormat.parse(dateStr);
                                
                                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH);
                                String monthKey = monthFormat.format(date);
                                
                                if ("ABSENCE".equalsIgnoreCase(type)) {
                                    absencesByMonth.put(monthKey, absencesByMonth.getOrDefault(monthKey, 0) + 1);
                                    absenceDetails.add(new AbsenceDetail(dateStr, heureStr, salle));
                                } else if ("RETARD".equalsIgnoreCase(type)) {
                                    retardsByMonth.put(monthKey, retardsByMonth.getOrDefault(monthKey, 0) + 1);
                                    // Ajouter une durée par défaut de 0 minutes pour les retards
                                    retardDetails.add(new RetardDetail(dateStr, heureStr, salle, 0));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // Organiser par mois avec les absences et retards
                    Map<String, MonthStats> monthlyStats = organizeByMonth(surveillantPointages, absencesByMonth, retardsByMonth);
                    generatePdfDocument(nomSurveillant, monthlyStats, surveillantPointages, absenceDetails, retardDetails, listener);
                }

                @Override
                public void onError(Exception error) {
                    // Même en cas d'erreur, générer le PDF sans les absences et retards
                    Map<String, MonthStats> monthlyStats = organizeByMonth(surveillantPointages, new HashMap<>(), new HashMap<>());
                    generatePdfDocument(nomSurveillant, monthlyStats, surveillantPointages, new ArrayList<>(), new ArrayList<>(), listener);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // En cas d'erreur, générer le PDF sans les absences et retards
            Map<String, MonthStats> monthlyStats = organizeByMonth(surveillantPointages, new HashMap<>(), new HashMap<>());
            generatePdfDocument(nomSurveillant, monthlyStats, surveillantPointages, new ArrayList<>(), new ArrayList<>(), listener);
        }
    }

    private void generatePdfDocument(String nomSurveillant, Map<String, MonthStats> monthlyStats, List<Pointage> surveillantPointages, List<AbsenceDetail> absenceDetails, List<RetardDetail> retardDetails, OnPdfGeneratedListener listener) {

        // Créer le PDF
        try {
            Uri pdfUri = createPdfUri(nomSurveillant);
            if (pdfUri == null) return;

            ContentResolver resolver = context.getContentResolver();
            try (OutputStream outputStream = resolver.openOutputStream(pdfUri)) {
                if (outputStream == null) {
                    Toast.makeText(context, "Erreur lors de la création du PDF", Toast.LENGTH_SHORT).show();
                    return;
                }

                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdfDocument = new PdfDocument(writer);
                Document document = new Document(pdfDocument);

                // En-tête
                addHeader(document, nomSurveillant);

                // Tableau des statistiques mensuelles
                addMonthlyStatsTable(document, monthlyStats);

                // Détails des retards et absences avec date/heure/salle
                addDetailedRetardsAndAbsences(document, surveillantPointages, absenceDetails, retardDetails);

                // Résumé total
                addTotalSummary(document, surveillantPointages, monthlyStats);

                document.close();
                Toast.makeText(context, "PDF généré avec succès dans Documents/Pointage", Toast.LENGTH_LONG).show();
                
                if (listener != null) {
                    listener.onPdfGenerated(pdfUri, nomSurveillant);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }
    

    private Uri createPdfUri(String nomSurveillant) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        String fileName = "Rapport_" + nomSurveillant.replace(" ", "_") + "_" + 
                          System.currentTimeMillis() + ".pdf";

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, 
                         Environment.DIRECTORY_DOCUMENTS + "/Pointage");

        return resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
    }

    private void addHeader(Document document, String nomSurveillant) {
        Paragraph title = new Paragraph("RAPPORT DE POINTAGE")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        Paragraph surveillantName = new Paragraph("Surveillant : " + nomSurveillant)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(surveillantName);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Paragraph date = new Paragraph("Date de génération : " + dateFormat.format(Calendar.getInstance().getTime()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
        document.add(date);
    }

    private void addMonthlyStatsTable(Document document, Map<String, MonthStats> monthlyStats) {
        Paragraph sectionTitle = new Paragraph("Historique Mensuel")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(sectionTitle);

        // Créer un tableau avec 5 colonnes : Mois, Présences, Absences, Retards, Total
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}))
                .useAllAvailableWidth();

        // En-tête du tableau
        DeviceRgb headerColor = new DeviceRgb(26, 35, 126); // Bleu foncé
        table.addHeaderCell(createHeaderCell("Mois"));
        table.addHeaderCell(createHeaderCell("Présences"));
        table.addHeaderCell(createHeaderCell("Absences"));
        table.addHeaderCell(createHeaderCell("Retards"));
        table.addHeaderCell(createHeaderCell("Total"));

        // Trier les mois chronologiquement
        List<String> sortedMonths = new ArrayList<>(monthlyStats.keySet());
        sortedMonths.sort((a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            int yearCompare = Integer.compare(Integer.parseInt(partsA[1]), Integer.parseInt(partsB[1]));
            if (yearCompare != 0) return yearCompare;
            return getMonthNumber(partsA[0]) - getMonthNumber(partsB[0]);
        });

        // Remplir le tableau
        for (String month : sortedMonths) {
            MonthStats stats = monthlyStats.get(month);
            table.addCell(createCell(month));
            table.addCell(createCell(String.valueOf(stats.presences)));
            table.addCell(createCell(String.valueOf(stats.absences)));
            table.addCell(createCell(String.valueOf(stats.retards)));
            table.addCell(createCell(String.valueOf(stats.presences + stats.absences)));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addDetailedRetardsAndAbsences(Document document, List<Pointage> pointages, List<AbsenceDetail> absenceDetails, List<RetardDetail> retardDetails) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Section Retards - maintenant chargés depuis la table sanction
        if (!retardDetails.isEmpty()) {
            Paragraph retardsTitle = new Paragraph("Détail des Retards")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(retardsTitle);
            
            Table retardsTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}))
                    .useAllAvailableWidth();
            
            retardsTable.addHeaderCell(createHeaderCell("Date"));
            retardsTable.addHeaderCell(createHeaderCell("Session"));
            retardsTable.addHeaderCell(createHeaderCell("Salle"));
            
            for (RetardDetail retard : retardDetails) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(retard.date);
                    String formattedDate = date != null ? dateFormat.format(date) : retard.date;
                    
                    retardsTable.addCell(createCell(formattedDate));
                    retardsTable.addCell(createCell(retard.heure));
                    retardsTable.addCell(createCell(retard.salle));
                } catch (Exception e) {
                    retardsTable.addCell(createCell(retard.date));
                    retardsTable.addCell(createCell(retard.heure));
                    retardsTable.addCell(createCell(retard.salle));
                }
            }
            
            document.add(retardsTable);
            document.add(new Paragraph("\n"));
        }
        
        // Section Absences
        if (!absenceDetails.isEmpty()) {
            Paragraph absencesTitle = new Paragraph("Détail des Absences")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(absencesTitle);
            
            Table absencesTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}))
                    .useAllAvailableWidth();
            
            absencesTable.addHeaderCell(createHeaderCell("Date"));
            absencesTable.addHeaderCell(createHeaderCell("Session"));
            absencesTable.addHeaderCell(createHeaderCell("Salle"));
            
            for (AbsenceDetail absence : absenceDetails) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(absence.date);
                    String formattedDate = date != null ? dateFormat.format(date) : absence.date;
                    
                    absencesTable.addCell(createCell(formattedDate));
                    absencesTable.addCell(createCell(absence.heure));
                    absencesTable.addCell(createCell(absence.salle));
                } catch (Exception e) {
                    absencesTable.addCell(createCell(absence.date));
                    absencesTable.addCell(createCell(absence.heure));
                    absencesTable.addCell(createCell(absence.salle));
                }
            }
            
            document.add(absencesTable);
            document.add(new Paragraph("\n"));
        }
    }

    private void addTotalSummary(Document document, List<Pointage> pointages, Map<String, MonthStats> monthlyStats) {
        int totalRetards = 0;
        int totalAbsences = 0;
        int totalPresences = 0;

        for (Pointage p : pointages) {
            if (p.isRetard()) {
                totalRetards++;
            }
            totalPresences++;
        }

        // Calculer le total des absences depuis monthlyStats
        for (MonthStats stats : monthlyStats.values()) {
            totalAbsences += stats.absences;
        }

        Paragraph summaryTitle = new Paragraph("Résumé Général")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(summaryTitle);

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        DeviceRgb summaryColor = new DeviceRgb(232, 234, 246);
        
        summaryTable.addCell(createSummaryCell("Total des présences :"));
        summaryTable.addCell(createSummaryCell(String.valueOf(totalPresences)));
        
        summaryTable.addCell(createSummaryCell("Total des retards :"));
        summaryTable.addCell(createSummaryCell(String.valueOf(totalRetards), new DeviceRgb(255, 152, 0))); // Orange
        
        summaryTable.addCell(createSummaryCell("Total des absences :"));
        summaryTable.addCell(createSummaryCell(String.valueOf(totalAbsences), new DeviceRgb(244, 67, 54))); // Rouge

        document.add(summaryTable);
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new DeviceRgb(26, 35, 126))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    // Méthode de création de cellule avec un style par défaut
    private Cell createCell(String text) {
        return createCell(text, false);
    }

    private Cell createSummaryCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setPadding(8);
    }

    private Cell createSummaryCell(String text, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(color))
                .setPadding(8);
    }

    private Map<String, MonthStats> organizeByMonth(List<Pointage> pointages, Map<String, Integer> absencesByMonth, Map<String, Integer> retardsByMonth) {
        Map<String, MonthStats> monthlyStats = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH);

        for (Pointage p : pointages) {
            if (p.getHeure_pointage() != null) {
                String monthKey = monthFormat.format(p.getHeure_pointage());
                
                if (!monthlyStats.containsKey(monthKey)) {
                    monthlyStats.put(monthKey, new MonthStats());
                }

                MonthStats stats = monthlyStats.get(monthKey);
                // Ne plus compter les retards depuis les pointages, ils viennent maintenant de la table sanction
                stats.presences++;
            }
        }

        // Ajouter les absences depuis la map
        for (Map.Entry<String, Integer> entry : absencesByMonth.entrySet()) {
            String monthKey = entry.getKey();
            if (!monthlyStats.containsKey(monthKey)) {
                monthlyStats.put(monthKey, new MonthStats());
            }
            monthlyStats.get(monthKey).absences = entry.getValue();
        }

        // Ajouter les retards depuis la map
        for (Map.Entry<String, Integer> entry : retardsByMonth.entrySet()) {
            String monthKey = entry.getKey();
            if (!monthlyStats.containsKey(monthKey)) {
                monthlyStats.put(monthKey, new MonthStats());
            }
            monthlyStats.get(monthKey).retards = entry.getValue();
        }

        return monthlyStats;
    }

    private int getMonthNumber(String monthName) {
        String[] months = {"janvier", "février", "mars", "avril", "mai", "juin",
                          "juillet", "août", "septembre", "octobre", "novembre", "décembre"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(monthName)) {
                return i;
            }
        }
        return 0;
    }

    private static class MonthStats {
        int presences = 0;
        int absences = 0;
        int retards = 0;
    }

    private static class AbsenceDetail {
        String date;
        String heure;
        String salle;

        AbsenceDetail(String date, String heure, String salle) {
            this.date = date;
            this.heure = heure;
            this.salle = salle;
        }
    }

    // Classe pour représenter les détails d'un retard
    private static class RetardDetail {
        String date;
        String heure;
        String salle;
        int duree;

        public RetardDetail(String date, String heure, String salle, int duree) {
            this.date = date;
            this.heure = heure;
            this.salle = salle;
            this.duree = duree;
        }
    }
    
    public interface OnGlobalPdfGeneratedListener {
        void onPdfGenerated(Uri pdfUri);
        void onError(String errorMessage);
    }
    
    public void generateGlobalReport(int month, int year, OnGlobalPdfGeneratedListener listener) {
        try {
            // Calculer les dates de début et de fin du mois
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String startDate = String.format(Locale.getDefault(), "%04d-%02d-01", year, month + 1);

            // Obtenir le dernier jour du mois
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH) + 1, 
                cal.get(Calendar.DAY_OF_MONTH));

            // Utiliser la syntaxe de filtre correcte pour Supabase
            String dateFilter = String.format(Locale.getDefault(), 
                "date_examen=gte.%s&date_examen=lte.%s", 
                startDate, endDate);
                
            // Log du filtre pour débogage
            android.util.Log.d("PdfGenerator", "Filtre de date: " + dateFilter);
                
            ConnectClient connectClient = ConnectClient.getInstance();
            // Utiliser le filtre dans la requête
            android.util.Log.d("PdfGenerator", "Début de la requête vers l'API...");
            connectClient.select("sanction", "*", dateFilter, new ConnectClient.ClientCallback() {
                @Override
                public void onSuccess(JsonArray result) {
                    try {
                        // Créer un document PDF
                        String fileName = "Rapport_Sanctions_" + getMonthName(month) + "_" + year + ".pdf";
                        ContentResolver resolver = context.getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        Uri collection = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        }
                        Uri pdfUri = resolver.insert(collection, contentValues);

                        if (pdfUri == null) {
                            listener.onError("Erreur lors de la création du fichier PDF");
                            return;
                        }

                        try (OutputStream outputStream = resolver.openOutputStream(pdfUri);
                             PdfWriter writer = new PdfWriter(outputStream);
                             PdfDocument pdf = new PdfDocument(writer);
                             Document document = new Document(pdf)) {

                            // Titre du document
                            Paragraph title = new Paragraph("Rapport des Sanctions")
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(20)
                                .setBold();
                            document.add(title);

                            // Période du rapport
                            Paragraph period = new Paragraph(String.format("Période : %s %d\n\n", getMonthName(month), year))
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(14);
                            document.add(period);

                            // Tableau des statistiques
                            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}));
                            table.setWidth(UnitValue.createPercentValue(100));

                            // En-têtes du tableau
                            addTableHeader(table, "Surveillant");
                            addTableHeader(table, "Absences");
                            addTableHeader(table, "Retards");
                            addTableHeader(table, "Total");

                            // Récupérer tous les surveillants uniques
                            Map<String, Integer> absences = new HashMap<>();
                            Map<String, Integer> retards = new HashMap<>();
                            Map<String, String> surveillants = new HashMap<>();

                            for (int i = 0; i < result.size(); i++) {
                                JsonObject sanction = result.get(i).getAsJsonObject();
                                String type = sanction.has("type") && !sanction.get("type").isJsonNull() ? 
                                    sanction.get("type").getAsString() : "";
                                String idSurveillant = sanction.has("id_surveillant") && !sanction.get("id_surveillant").isJsonNull() ? 
                                    sanction.get("id_surveillant").getAsString() : "";
                                String nomSurveillant = sanction.has("nom_surveillant") && !sanction.get("nom_surveillant").isJsonNull() ? 
                                    sanction.get("nom_surveillant").getAsString() : "Inconnu";

                                if (!idSurveillant.isEmpty()) {
                                    surveillants.put(idSurveillant, nomSurveillant);
                                    
                                    if ("ABSENCE".equalsIgnoreCase(type)) {
                                        absences.put(idSurveillant, absences.getOrDefault(idSurveillant, 0) + 1);
                                    } else if ("RETARD".equalsIgnoreCase(type)) {
                                        retards.put(idSurveillant, retards.getOrDefault(idSurveillant, 0) + 1);
                                    }
                                }
                            }

                            // Ajouter les données des surveillants au tableau
                            int totalAbsences = 0;
                            int totalRetards = 0;
                            
                            for (Map.Entry<String, String> entry : surveillants.entrySet()) {
                                String id = entry.getKey();
                                String nom = entry.getValue();
                                int nbAbsences = absences.getOrDefault(id, 0);
                                int nbRetards = retards.getOrDefault(id, 0);
                                
                                table.addCell(createCell(nom));
                                table.addCell(createCell(String.valueOf(nbAbsences)));
                                table.addCell(createCell(String.valueOf(nbRetards)));
                                table.addCell(createCell(String.valueOf(nbAbsences + nbRetards)));
                                
                                totalAbsences += nbAbsences;
                                totalRetards += nbRetards;
                            }

                            // Ajouter le total
                            //table.addCell(createCell("TOTAL", true));
                            //table.addCell(createCell(String.valueOf(totalAbsences), true));
                            //table.addCell(createCell(String.valueOf(totalRetards), true));
                            //table.addCell(createCell(String.valueOf(totalAbsences + totalRetards), true));

                            document.add(table);

                            // Pied de page
                            Paragraph footer = new Paragraph("Généré le " + 
                                new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(new Date()))
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(10)
                                .setItalic()
                                .setMarginTop(20);
                            document.add(footer);

                            // Notifier que le PDF est prêt
                            listener.onPdfGenerated(pdfUri);

                        } catch (Exception e) {
                            e.printStackTrace();
                            listener.onError("Erreur lors de la génération du PDF: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        String errorMsg = "Erreur lors du traitement des données: " + e.getMessage();
                        android.util.Log.e("PdfGenerator", errorMsg, e);
                        listener.onError(errorMsg);
                    }
                }

                @Override
                public void onError(Exception error) {
                    String errorMsg = "Erreur lors de la récupération des données: " + error.getMessage();
                    android.util.Log.e("PdfGenerator", errorMsg, error);
                    listener.onError(errorMsg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError("Erreur inattendue: " + e.getMessage());
        }
    }
    
    private void addTableHeader(Table table, String header) {
        table.addHeaderCell(new Cell()
            .add(new Paragraph(header).setBold())
            .setBackgroundColor(new DeviceRgb(63, 81, 181))
            .setFontColor(ColorConstants.WHITE)
            .setTextAlignment(TextAlignment.CENTER));
    }
    
    // Méthode unifiée pour créer une cellule avec option de style total
    
    private Cell createCell(String text, boolean isTotal) {
        return new Cell()
            .add(new Paragraph(text).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(isTotal ? new DeviceRgb(227, 242, 253) : ColorConstants.WHITE)
            .setPadding(8);
    }
    
    private String getMonthName(int month) {
        return new DateFormatSymbols(Locale.FRENCH).getMonths()[month];
    }
}
