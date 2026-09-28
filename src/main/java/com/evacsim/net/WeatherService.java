package com.evacsim.net;

import com.evacsim.model.HazardConditions;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Fetches live weather JSON (Open-Meteo) and maps it onto evacuation hazard.
 */
public final class WeatherService {

    private static final Map<String, double[]> CITIES = Map.of(
            "Dhaka", new double[]{23.8103, 90.4125},
            "Chittagong", new double[]{22.3569, 91.7832},
            "Sylhet", new double[]{24.8949, 91.8687}
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final Gson gson = new Gson();
    private String lastRawJson = "";

    public HazardConditions fetch(String city) throws Exception {
        double[] coords = CITIES.getOrDefault(city, CITIES.get("Dhaka"));
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,visibility",
                coords[0], coords[1]);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        lastRawJson = response.body();
        WeatherResponse parsed = gson.fromJson(lastRawJson, WeatherResponse.class);
        if (parsed == null || parsed.current == null) {
            throw new IllegalStateException("JSON missing current weather object");
        }
        return toHazard(city, parsed.current);
    }

    public String getLastRawJson() {
        return lastRawJson;
    }

    private static HazardConditions toHazard(String city, CurrentWeather current) {
        double hesitation = 0;
        if (current.weatherCode >= 51) {
            hesitation += 0.18;
        }
        if (current.windSpeed >= 20) {
            hesitation += 0.12;
        }
        if (current.visibility > 0 && current.visibility < 4000) {
            hesitation += 0.15;
        }
        hesitation = Math.min(0.4, hesitation);

        String summary = String.format(Locale.US,
                "%s · %.1f°C · wind %.1f km/h · vis %.0fm · code %d",
                city, current.temperature, current.windSpeed, current.visibility, current.weatherCode);
        return new HazardConditions(
                summary,
                current.temperature,
                current.weatherCode,
                current.windSpeed,
                current.visibility,
                hesitation);
    }

    public static final class WeatherResponse {
        CurrentWeather current;
    }

    public static final class CurrentWeather {
        @SerializedName("temperature_2m")
        double temperature;
        @SerializedName("relative_humidity_2m")
        double humidity;
        @SerializedName("weather_code")
        int weatherCode;
        @SerializedName("wind_speed_10m")
        double windSpeed;
        double visibility;
    }
}
