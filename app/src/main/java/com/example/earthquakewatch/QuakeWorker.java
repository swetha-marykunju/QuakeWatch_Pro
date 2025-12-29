package com.example.earthquakewatch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuakeWorker extends Worker {

    public QuakeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("QuakePrefs", Context.MODE_PRIVATE);
        String lastQuakeId = prefs.getString("last_id", "");

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://earthquake.usgs.gov/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService service = retrofit.create(ApiService.class);
            Response<EarthquakeResponse> response = service.getQuakes().execute();

            if (response.isSuccessful() && response.body() != null && !response.body().features.isEmpty()) {
                EarthquakeResponse.Feature latestQuake = response.body().features.get(0);
                String currentId = latestQuake.id;

                if (currentId == null) currentId = "no_id";

                double mag = latestQuake.properties.mag;

                if (!currentId.equals(lastQuakeId) && mag >= 5.0) {

                    showNotification(
                            "Significant Quake Detected!",
                            "Magnitude " + mag + " - " + latestQuake.properties.place
                    );

                    prefs.edit().putString("last_id", currentId).apply();
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    public void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "quake_alerts";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_quake_notifier)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}