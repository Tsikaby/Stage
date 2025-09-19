package com.example.pointage.ui.surveillant;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    private List<Surveillant> surveillantList;
    private List<Surveillant> originalList;
    private Context context;

    public SurveillantAdapter(List<Surveillant> surveillantList) {
        this.surveillantList = surveillantList;
        this.originalList = new ArrayList<>(surveillantList);
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
        return new SurveillantViewHolder(view, context);
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
        Button saveQrButton;
        private Context context;

        public SurveillantViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            nomSurveillantTextView = itemView.findViewById(R.id.nom_surveillant_text_view);
            contactTextView = itemView.findViewById(R.id.contact_text_view);
            idSurveillantTextView = itemView.findViewById(R.id.id_surveillant_text_view);
            idSalleTextView = itemView.findViewById(R.id.numero_salle_text_view);
            qrCodeImageView = itemView.findViewById(R.id.qr_code_image_view);
            saveQrButton = itemView.findViewById(R.id.save_qr_button);
        }

        public void bind(Surveillant surveillant) {
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

                Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 200, 200);
                qrCodeImageView.setImageBitmap(bitmap);

                saveQrButton.setOnClickListener(v -> saveImage(bitmap, surveillant.getNom_surveillant()));

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

    }
}