package com.example.pointage.ui.pending;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pointage.ConnectClient;
import com.example.pointage.ui.historique.DateUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PendingViewModel extends ViewModel {

    private final MutableLiveData<List<PendingItem>> pendingLiveData = new MutableLiveData<>();
    private final ConnectClient connectClient = ConnectClient.getInstance();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final long refreshIntervalMs = 15000; // 15s
    private volatile boolean isLoading = false;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadPending();
            refreshHandler.postDelayed(this, refreshIntervalMs);
        }
    };

    public PendingViewModel() throws UnknownHostException {
        loadPending();
        // Start auto-refresh
        refreshHandler.postDelayed(refreshRunnable, refreshIntervalMs);
    }

    public LiveData<List<PendingItem>> getPending() {
        return pendingLiveData;
    }

    private String getSession(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour < 12 ? "Matin" : "Après-midi";
    }

    public void loadPending() {
        if (isLoading) return;
        isLoading = true;
        final Date now = new Date();
        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now);

        final Map<Long, String> surveillantNames = new HashMap<>();
        // Track statuses per (surveillant, session) using key: id|session
        final Set<String> pointedKeys = new HashSet<>();
        final Set<String> absentKeys = new HashSet<>();

        // 1) Load surveillant names
        connectClient.select("surveillant", "*", null, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject s = result.get(i).getAsJsonObject();
                        if (!s.has("id_surveillant") || s.get("id_surveillant").isJsonNull()) continue;
                        long id = s.get("id_surveillant").getAsLong();
                        String name = s.has("nom_surveillant") && !s.get("nom_surveillant").isJsonNull() ? s.get("nom_surveillant").getAsString() : null;
                        if (name != null) surveillantNames.put(id, name);
                    }
                    loadPointed(today, surveillantNames, pointedKeys, absentKeys);
                } catch (Exception e) {
                    pendingLiveData.setValue(new ArrayList<>());
                }
            }
            @Override
            public void onError(Exception error) {
                pendingLiveData.setValue(new ArrayList<>());
                isLoading = false;
            }
        });
    }

    private void loadPointed(String today, Map<Long, String> surveillantNames,
                             Set<String> pointedKeys, Set<String> absentKeys) {
        String startDate = today + "T00:00:00";
        String endDate = today + "T23:59:59";
        String pointageFilter = "heure_pointage=gte." + startDate + "&heure_pointage=lte." + endDate;
        connectClient.select("pointage", "*", pointageFilter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject p = result.get(i).getAsJsonObject();
                        if (!p.has("id_surveillant") || p.get("id_surveillant").isJsonNull()) continue;
                        long id = p.get("id_surveillant").getAsLong();
                        String ts = p.get("heure_pointage").getAsString();
                        Date d = DateUtils.parseSupabaseTimestamp(ts);
                        String sess = getSession(d);
                        pointedKeys.add(id + "|" + sess);
                    }
                } catch (Exception ignored) {}
                loadAbsents(today, surveillantNames, pointedKeys, absentKeys);
            }
            @Override
            public void onError(Exception error) {
                loadAbsents(today, surveillantNames, pointedKeys, absentKeys);
            }
        });
    }

    private void loadAbsents(String today, Map<Long, String> surveillantNames,
                             Set<String> pointedKeys, Set<String> absentKeys) {
        // Load absences for both sessions today
        String filter = "type=eq.ABSENCE&date_examen=eq." + today;
        connectClient.select("sanction", "*", filter, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray result) {
                try {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject s = result.get(i).getAsJsonObject();
                        if (!s.has("id_surveillant") || s.get("id_surveillant").isJsonNull()) continue;
                        long id = s.get("id_surveillant").getAsLong();
                        String sess = s.has("session") && !s.get("session").isJsonNull() ? s.get("session").getAsString() : null;
                        if (sess != null && !sess.isEmpty()) {
                            absentKeys.add(id + "|" + sess);
                        }
                    }
                } catch (Exception ignored) {}
                loadPlanning(today, surveillantNames, pointedKeys, absentKeys);
            }
            @Override
            public void onError(Exception error) {
                loadPlanning(today, surveillantNames, pointedKeys, absentKeys);
            }
        });
    }

    private void loadPlanning(String today, Map<Long, String> surveillantNames,
                              Set<String> pointedKeys, Set<String> absentKeys) {
        connectClient.select("planning_surveillance", "*", null, new ConnectClient.ClientCallback() {
            @Override
            public void onSuccess(JsonArray planningResult) {
                List<PendingItem> items = new ArrayList<>();
                try {
                    // Aggregate earliest exam per (surveillant, session)
                    Map<String, Date> earliestByKey = new HashMap<>(); // key=id|session
                    Map<String, String> roomByKey = new HashMap<>();
                    for (int i = 0; i < planningResult.size(); i++) {
                        JsonObject p = planningResult.get(i).getAsJsonObject();
                        if (!p.has("id_surveillant") || p.get("id_surveillant").isJsonNull()) continue;
                        long idSurv = p.get("id_surveillant").getAsLong();
                        String heureDebutStr = p.has("heure_debut") && !p.get("heure_debut").isJsonNull() ? p.get("heure_debut").getAsString() : null;
                        String numeroSalle = p.has("numero_salle") && !p.get("numero_salle").isJsonNull() ? p.get("numero_salle").getAsString() : null;
                        if (heureDebutStr == null || numeroSalle == null) continue;

                        Date heureDebut;
                        try {
                            heureDebut = DateUtils.parseSupabaseTimestamp(heureDebutStr);
                        } catch (ParseException e) {
                            // fallback if stored as date+time
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                heureDebut = sdf.parse(heureDebutStr.replace('T',' ').substring(0,19));
                            } catch (Exception ex) { continue; }
                        }

                        // Filter by same day; include both sessions
                        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(heureDebut);
                        if (!today.equals(day)) continue;
                        String sess = getSession(heureDebut);

                        String key = idSurv + "|" + sess;
                        Date current = earliestByKey.get(key);
                        if (current == null || heureDebut.before(current)) {
                            earliestByKey.put(key, heureDebut);
                            roomByKey.put(key, numeroSalle);
                        }
                    }

                    // Build final list excluding pointed/absent per session and keeping earliest
                    for (Map.Entry<String, Date> entry : earliestByKey.entrySet()) {
                        String key = entry.getKey();
                        Date earliest = entry.getValue();
                        if (pointedKeys.contains(key)) continue;
                        if (absentKeys.contains(key)) continue;

                        String[] parts = key.split("\\|");
                        if (parts.length < 2) continue;
                        long idSurv = Long.parseLong(parts[0]);
                        String sess = parts[1];
                        String name = surveillantNames.get(idSurv);
                        if (name == null) continue;
                        String room = roomByKey.get(key);
                        String deadline = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(earliest);
                        items.add(new PendingItem(idSurv, name, room, deadline, sess));
                    }
                    // Sort: session (Matin before Après-midi), then time ascending
                    java.util.Collections.sort(items, (a, b) -> {
                        int sa = "Matin".equalsIgnoreCase(a.getSession()) ? 0 : 1;
                        int sb = "Matin".equalsIgnoreCase(b.getSession()) ? 0 : 1;
                        if (sa != sb) return sa - sb;
                        return a.getDeadline().compareTo(b.getDeadline());
                    });
                } catch (Exception ignored) {}
                pendingLiveData.setValue(items);
                isLoading = false;
            }
            @Override
            public void onError(Exception error) {
                pendingLiveData.setValue(new ArrayList<>());
                isLoading = false;
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        refreshHandler.removeCallbacksAndMessages(null);
    }
}
