package com.example.earthquakewatch;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EarthquakeResponse {
    @SerializedName("features")
    public List<Feature> features;

    public static class Feature {
        @SerializedName("properties")
        public Props properties;
        @SerializedName("geometry")
        public Geo geometry;
        @SerializedName("id")
        public String id;

    }
    public static class Props {
        public double mag;
        public String place;
        public long time;
        public int tsunami;
    }
    public static class Geo {
        public List<Double> coordinates;
    }
}