package com.example.pointage;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class SupabaseClient {
    private static SupabaseClient instance;
    private OkHttpClient httpClient;
    private Handler mainHandler;

    // Remplacez ces valeurs par vos propres clés Supabase
    private static final String SUPABASE_URL = "https://sidshqdnmtccxgfzzrve.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc";
    private static final String SUPABASE_API_URL = SUPABASE_URL + "/rest/v1/";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private Gson gson;

    private SupabaseClient() throws UnknownHostException {
        // Bootstrap client pour DoH avec timeouts courts
        OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        // Cloudflare DoH
        DnsOverHttps dohCloudflare = new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                .bootstrapDnsHosts(
                        InetAddress.getByName("1.1.1.1"),
                        InetAddress.getByName("1.0.0.1"),
                        InetAddress.getByName("2606:4700:4700::1111"),
                        InetAddress.getByName("2606:4700:4700::1001")
                )
                .build();

        // Google DoH (fallback supplémentaire)
        DnsOverHttps dohGoogle = new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(HttpUrl.get("https://dns.google/dns-query"))
                .bootstrapDnsHosts(
                        InetAddress.getByName("8.8.8.8"),
                        InetAddress.getByName("8.8.4.4"),
                        InetAddress.getByName("2001:4860:4860::8888"),
                        InetAddress.getByName("2001:4860:4860::8844")
                )
                .build();

        // Résolution résiliente: Système → Cloudflare → Google
        Dns resilientDns = hostname -> {
            try { return Dns.SYSTEM.lookup(hostname); }
            catch (UnknownHostException e1) {
                try { return dohCloudflare.lookup(hostname); }
                catch (UnknownHostException e2) {
                    return dohGoogle.lookup(hostname);
                }
            }
        };

        httpClient = new OkHttpClient.Builder()
                .dns(resilientDns)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized SupabaseClient getInstance() throws UnknownHostException {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public void select(String table, String select, String filter, SupabaseCallback callback) {
        String url = SUPABASE_API_URL + table;
        if (select != null) {
            url += "?select=" + select;
        }
        if (filter != null) {
            url += (select != null ? "&" : "?") + filter;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        mainHandler.post(() -> {
                            try {
                                callback.onSuccess(jsonArray);
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + response.code())));
                }
            }
        });
    }

    public void insert(String table, JsonObject data, SupabaseCallback callback) {
        String url = SUPABASE_API_URL + table;

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        mainHandler.post(() -> {
                            try {
                                callback.onSuccess(jsonArray);
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                } else {
                    String errorBody = null;
                    try {
                        errorBody = response.body() != null ? response.body().string() : null;
                    } catch (Exception ignored) {}
                    String message = "HTTP Error: " + response.code() + (errorBody != null && !errorBody.isEmpty() ? " - " + errorBody : "");
                    final String finalMessage = message;
                    mainHandler.post(() -> callback.onError(new Exception(finalMessage)));
                }
            }
        });
    }

    public void update(String table, String filter, JsonObject data, SupabaseCallback callback) {
        String url = SUPABASE_API_URL + table;
        if (filter != null) {
            url += "?" + filter;
        }

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        mainHandler.post(() -> {
                            try {
                                callback.onSuccess(jsonArray);
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + response.code())));
                }
            }
        });
    }

    public void delete(String table, String filter, SupabaseCallback callback) {
        String url = SUPABASE_API_URL + table;
        if (filter != null) {
            url += "?" + filter;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .delete()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> {
                        try {
                            callback.onSuccess(new JsonArray());
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + response.code())));
                }
            }
        });
    }

    public interface SupabaseCallback {
        void onSuccess(JsonArray result) throws UnknownHostException;
        void onError(Exception error);
    }
}