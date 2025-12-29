package com.example.earthquakewatch;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("fdsnws/event/1/query?format=geojson&minmagnitude=4.0")
    Call<EarthquakeResponse> getQuakes();
}

