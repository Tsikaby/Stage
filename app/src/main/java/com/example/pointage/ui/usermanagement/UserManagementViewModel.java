package com.example.pointage.ui.usermanagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ConnectClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UserManagementViewModel extends ViewModel {

    private final MutableLiveData<List<PendingUser>> pendingUsersLiveData = new MutableLiveData<>();
    private final ConnectClient connectClient;

    public interface OnUserActionListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public UserManagementViewModel() throws UnknownHostException {
        connectClient = ConnectClient.getInstance();
    }

    public LiveData<List<PendingUser>> getPendingUsers() {
        return pendingUsersLiveData;
    }

    public void loadPendingUsers() {
        String filter = "approved=eq.false";
        connectClient.select("utilisateurs", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                List<PendingUser> pendingUsers = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    JsonObject user = result.get(i).getAsJsonObject();
                    String username = user.get("username").getAsString();
                    String role = user.has("role") && !user.get("role").isJsonNull() ?
                            user.get("role").getAsString() : "user";

                    pendingUsers.add(new PendingUser(username, role));
                }
                pendingUsersLiveData.setValue(pendingUsers);
            }

            @Override
            public void onError(Exception error) {
                pendingUsersLiveData.setValue(new ArrayList<>());
            }
        });
    }

    public void approveUser(String username, OnUserActionListener listener) {
        JsonObject body = new JsonObject();
        body.addProperty("approved", true);
        String filter = "username=eq." + username;

        connectClient.update("utilisateurs", filter, body, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                listener.onSuccess("Utilisateur " + username + " approuvé avec succès");
            }

            @Override
            public void onError(Exception error) {
                listener.onError("Erreur lors de l'approbation: " + error.getMessage());
            }
        });
    }

    public void rejectUser(String username, OnUserActionListener listener) {
        String filter = "username=eq." + username;
        connectClient.delete("utilisateurs", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                listener.onSuccess("Utilisateur " + username + " rejeté et supprimé");
            }

            @Override
            public void onError(Exception error) {
                listener.onError("Erreur lors du rejet: " + error.getMessage());
            }
        });
    }

    public static class PendingUser {
        private String username;
        private String role;

        public PendingUser(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}











