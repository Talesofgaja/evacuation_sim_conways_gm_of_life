package com.evacsim.model;

/**
 * Live weather / visibility hazard applied to movement rules.
 */
public final class HazardConditions {

    public static final HazardConditions CLEAR =
            new HazardConditions("Clear indoor conditions", 24.0, 0, 0.0, 24_000, 0.0);

    private final String summary;
    private final double temperatureC;
    private final int weatherCode;
    private final double windKmh;
    private final double visibilityM;
    private final double hesitationExtra;

    public HazardConditions(String summary, double temperatureC, int weatherCode,
                            double windKmh, double visibilityM, double hesitationExtra) {
        this.summary = summary;
        this.temperatureC = temperatureC;
        this.weatherCode = weatherCode;
        this.windKmh = windKmh;
        this.visibilityM = visibilityM;
        this.hesitationExtra = hesitationExtra;
    }

    public String getSummary() {
        return summary;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public double getWindKmh() {
        return windKmh;
    }

    public double getVisibilityM() {
        return visibilityM;
    }

    public double getHesitationExtra() {
        return hesitationExtra;
    }
}
