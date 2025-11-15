package com.example.pointage.ui.surveillant;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pointage.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SurveillantAdapter extends RecyclerView.Adapter<SurveillantAdapter.SurveillantViewHolder> {

    public interface OnGeneratePdfListener {
        void onGeneratePdf(int idSurveillant, String nomSurveillant);
    }

    private List<Surveillant> surveillantList;
    private List<Surveillant> originalList;
    private Context context;
    private OnGeneratePdfListener pdfListener;

    public SurveillantAdapter(List<Surveillant> surveillantList) {
        this.surveillantList = surveillantList;
        this.originalList = new ArrayList<>(surveillantList);
    }

    public void setOnGeneratePdfListener(OnGeneratePdfListener listener) {
        this.pdfListener = listener;
    }

    public void setSurveillantList(List<Surveillant> newList) {
        this.surveillantList = newList;
        this.originalList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        surveillantList.clear();

        if (query.isEmpty()) {
            surveillantList.addAll(originalList);
        } else {
            query = query.toLowerCase().trim();
            for (Surveillant surveillant : originalList) {
                if (surveillant.getNom_surveillant().toLowerCase().contains(query)) {
                    surveillantList.add(surveillant);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SurveillantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surveillant, parent, false);
        return new SurveillantViewHolder(view, context, pdfListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveillantViewHolder holder, int position) {
        Surveillant surveillant = surveillantList.get(position);
        holder.bind(surveillant);
    }

    @Override
    public int getItemCount() {
        return surveillantList != null ? surveillantList.size() : 0;
    }

    static class SurveillantViewHolder extends RecyclerView.ViewHolder {
        TextView nomSurveillantTextView, contactTextView, idSurveillantTextView, idSalleTextView;
        ImageView qrCodeImageView;
        Button saveQrButton, shareWhatsappButton, shareMessengerButton, shareEmailButton, generatePdfButton, toggleShareButton;
        Button showQrButton, showActionsButton, closeQrButton, closeActionsButton;
        LinearLayout shareButtonsContainer, actionsButtonsContainer, buttonsRow;
        android.view.View qrContainer, actionsContainer;
        private Context context;
        private Bitmap currentQrBitmap;
        private Surveillant currentSurveillant;
        private OnGeneratePdfListener pdfListener;
        private boolean shareButtonsVisible = false;

        public SurveillantViewHolder(@NonNull View itemView, Context context, OnGeneratePdfListener pdfListener) {
            super(itemView);
            this.context = context;
            this.pdfListener = pdfListener;
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_text_view);
            contactTextView = itemView.findViewById(R.id.contact_text_view);
            idSurveillantTextView = itemView.findViewById(R.id.id_surveillant_text_view);
            idSalleTextView = itemView.findViewById(R.id.numero_salle_text_view);
            qrCodeImageView = itemView.findViewById(R.id.qr_code_image_view);
            saveQrButton = itemView.findViewById(R.id.save_qr_button);
            shareWhatsappButton = itemView.findViewById(R.id.share_whatsapp_button);
            shareMessengerButton = itemView.findViewById(R.id.share_messenger_button);
            shareEmailButton = itemView.findViewById(R.id.share_email_button);
            generatePdfButton = itemView.findViewById(R.id.generate_pdf_button);
            toggleShareButton = itemView.findViewById(R.id.toggle_share_button);
            showQrButton = itemView.findViewById(R.id.show_qr_button);
            showActionsButton = itemView.findViewById(R.id.show_actions_button);
            closeQrButton = itemView.findViewById(R.id.close_qr_button);
            closeActionsButton = itemView.findViewById(R.id.close_actions_button);
            shareButtonsContainer = itemView.findViewById(R.id.share_buttons_container);
            actionsButtonsContainer = itemView.findViewById(R.id.actions_buttons_container);
            qrContainer = itemView.findViewById(R.id.qr_container);
            actionsContainer = itemView.findViewById(R.id.actions_container);
            buttonsRow = itemView.findViewById(R.id.buttons_row);
        }

        public void bind(Surveillant surveillant) {
            this.currentSurveillant = surveillant;
            nomSurveillantTextView.setText("Nom: " + surveillant.getNom_surveillant());
            contactTextView.setText("Contact: " + surveillant.getContact());
            idSurveillantTextView.setText("ID Surveillant: " + surveillant.getId_surveillant());
            idSalleTextView.setText("Numéro de salle: " + surveillant.getNumero_salle());

            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                String qrData = "ID Surveillant: " + surveillant.getId_surveillant() + "\n" +
                        "Nom: " + surveillant.getNom_surveillant() + "\n" +
                        "Contact: " + surveillant.getContact() + "\n" +
                        "Numéro de salle: " + surveillant.getNumero_salle();

                Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 480, 480);
                this.currentQrBitmap = bitmap;
                qrCodeImageView.setImageBitmap(bitmap);

                // Configuration des listeners
                showQrButton.setOnClickListener(v -> showQrCode());
                showActionsButton.setOnClickListener(v -> showActions());
                closeQrButton.setOnClickListener(v -> hideQrCode());
                closeActionsButton.setOnClickListener(v -> hideActions());
                saveQrButton.setOnClickListener(v -> saveImage(bitmap, surveillant.getNom_surveillant()));
                shareEmailButton.setOnClickListener(v -> shareViaEmail());
                toggleShareButton.setOnClickListener(v -> shareViaEmail()); // Lier directement au partage par email
                generatePdfButton.setOnClickListener(v -> {
                    if (pdfListener != null) {
                        pdfListener.onGeneratePdf(surveillant.getId_surveillant(), surveillant.getNom_surveillant());
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void saveImage(Bitmap bitmap, String name) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            String fileName = "QR_" + name.replace(" ", "_") + "_" + System.currentTimeMillis() + ".png";

            // Configuration des valeurs pour MediaStore
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PointageQR");

            // Enregistrement de l'image
            Uri imageUri = null;
            try {
                // Obtenir l'URI de l'image pour y écrire
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null) throw new IOException("Failed to create new MediaStore record.");

                try (OutputStream fos = resolver.openOutputStream(Objects.requireNonNull(imageUri))) {
                    if (fos == null) throw new IOException("Failed to get output stream.");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    Toast.makeText(context, "QR code enregistré dans la galerie!", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                if (imageUri != null) {
                    resolver.delete(imageUri, null, null);
                }
                e.printStackTrace();
                Toast.makeText(context, "Erreur lors de l'enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private Uri saveImageToShare() {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            String fileName = "QR_" + currentSurveillant.getNom_surveillant().replace(" ", "_") + "_" + System.currentTimeMillis() + ".png";

            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PointageQR");

            Uri imageUri = null;
            try {
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null) throw new IOException("Failed to create new MediaStore record.");

                try (OutputStream fos = resolver.openOutputStream(Objects.requireNonNull(imageUri))) {
                    if (fos == null) throw new IOException("Failed to get output stream.");
                    currentQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                return imageUri;
            } catch (IOException e) {
                if (imageUri != null) {
                    resolver.delete(imageUri, null, null);
                }
                e.printStackTrace();
                Toast.makeText(context, "Erreur lors de la préparation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        private void showQrCode() {
            qrContainer.setVisibility(View.VISIBLE);
            actionsContainer.setVisibility(View.GONE);
            // Cacher les boutons d'affichage pendant que le QR code est affiché
            buttonsRow.setVisibility(View.GONE);
        }

        private void hideQrCode() {
            qrContainer.setVisibility(View.GONE);
            // Réafficher la rangée des boutons
            buttonsRow.setVisibility(View.VISIBLE);
        }

        private void showActions() {
            actionsContainer.setVisibility(View.VISIBLE);
            qrContainer.setVisibility(View.GONE);
            // Cacher les boutons d'affichage pendant que les actions sont affichées
            buttonsRow.setVisibility(View.GONE);
        }

        private void hideActions() {
            actionsContainer.setVisibility(View.GONE);
            // Réafficher la rangée des boutons
            buttonsRow.setVisibility(View.VISIBLE);
        }

        private void shareViaEmail() {
            Uri imageUri = saveImageToShare();
            if (imageUri == null) return;

            String subject = "QR Code - " + currentSurveillant.getNom_surveillant();
            String body = "Voici le QR Code pour:\n\n" +
                    "Nom: " + currentSurveillant.getNom_surveillant() + "\n" +
                    "Contact: " + currentSurveillant.getContact();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                context.startActivity(Intent.createChooser(intent, "Envoyer via Email"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "Aucune application email trouvée", Toast.LENGTH_SHORT).show();
            }
        }

    }
}