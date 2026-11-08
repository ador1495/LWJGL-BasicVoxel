package com.engine.world;

import org.joml.Vector3f;

public class DaylightCycle {
    // Duration of full 24h day-night cycle in seconds
    public static final float DAY_DURATION = 120.0f; // 2 minutes

    private float timeOfDay = 0.22f; // Start at morning (0.0 = sunrise, 0.25 = noon, 0.5 = sunset, 0.75 = midnight)
    private boolean paused = false;

    private final Vector3f sunDir = new Vector3f();
    private final Vector3f lightDir = new Vector3f();
    private final Vector3f lightColor = new Vector3f();
    private final Vector3f ambientColor = new Vector3f();
    private final Vector3f skyColor = new Vector3f();

    public DaylightCycle() {
        update(0);
    }

    public void update(float dt) {
        if (!paused && dt > 0) {
            timeOfDay += dt / DAY_DURATION;
            if (timeOfDay >= 1.0f) {
                timeOfDay -= 1.0f;
            }
        }

        // theta: angle along celestial arc
        // 0 at noon, PI/2 at sunset, PI at midnight, 3*PI/2 at sunrise
        float theta = (timeOfDay - 0.25f) * 2.0f * (float) Math.PI;

        float cosTheta = (float) Math.cos(theta);
        float sinTheta = (float) Math.sin(theta);

        // Sun position in sky (cosTheta = elevation, sinTheta = azimuth)
        sunDir.set(sinTheta, cosTheta, 0.25f).normalize();

        float elevation = cosTheta;

        // Light direction: comes from sun during day, moon during night
        if (elevation >= -0.05f) {
            lightDir.set(sunDir);
        } else {
            lightDir.set(-sunDir.x, -sunDir.y, -sunDir.z);
        }

        calculateColors(elevation);
    }

    private void calculateColors(float elevation) {
        // 1. Sky / Fog Color
        if (elevation > 0.25f) {
            // High noon to afternoon
            float t = Math.min(1.0f, (elevation - 0.25f) / 0.50f);
            lerp(new Vector3f(0.55f, 0.72f, 0.92f), new Vector3f(0.48f, 0.70f, 0.98f), t, skyColor);
        } else if (elevation > -0.05f) {
            // Sunrise / Sunset golden hour
            float t = (elevation + 0.05f) / 0.30f;
            lerp(new Vector3f(0.88f, 0.45f, 0.25f), new Vector3f(0.55f, 0.72f, 0.92f), t, skyColor);
        } else if (elevation > -0.25f) {
            // Twilight
            float t = (elevation + 0.25f) / 0.20f;
            lerp(new Vector3f(0.06f, 0.08f, 0.16f), new Vector3f(0.88f, 0.45f, 0.25f), t, skyColor);
        } else {
            // Midnight starry navy blue
            skyColor.set(0.04f, 0.06f, 0.12f);
        }

        // 2. Light Color (Direct sunlight / moonlight)
        if (elevation > 0.15f) {
            // Warm bright sunlight
            float t = Math.min(1.0f, (elevation - 0.15f) / 0.60f);
            lerp(new Vector3f(1.0f, 0.85f, 0.65f), new Vector3f(1.0f, 0.98f, 0.92f), t, lightColor);
        } else if (elevation > -0.05f) {
            // Sunset / Sunrise warm gold-red
            float t = (elevation + 0.05f) / 0.20f;
            lerp(new Vector3f(0.95f, 0.50f, 0.22f), new Vector3f(1.0f, 0.85f, 0.65f), t, lightColor);
        } else {
            // Cool moonlight
            lightColor.set(0.20f, 0.25f, 0.38f);
        }

        // 3. Ambient Color
        if (elevation > 0.20f) {
            float t = Math.min(1.0f, (elevation - 0.20f) / 0.50f);
            lerp(new Vector3f(0.30f, 0.32f, 0.35f), new Vector3f(0.38f, 0.40f, 0.44f), t, ambientColor);
        } else if (elevation > -0.10f) {
            float t = (elevation + 0.10f) / 0.30f;
            lerp(new Vector3f(0.15f, 0.14f, 0.18f), new Vector3f(0.30f, 0.32f, 0.35f), t, ambientColor);
        } else {
            // Night ambient: soft blue so blocks are visible
            ambientColor.set(0.09f, 0.11f, 0.16f);
        }
    }

    private static void lerp(Vector3f a, Vector3f b, float t, Vector3f dest) {
        dest.x = a.x + (b.x - a.x) * t;
        dest.y = a.y + (b.y - a.y) * t;
        dest.z = a.z + (b.z - a.z) * t;
    }

    public void advanceTime(float amount) {
        timeOfDay = (timeOfDay + amount) % 1.0f;
        if (timeOfDay < 0) timeOfDay += 1.0f;
        update(0);
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(float timeOfDay) {
        this.timeOfDay = timeOfDay;
        update(0);
    }

    public Vector3f getSunDir() {
        return sunDir;
    }

    public Vector3f getLightDir() {
        return lightDir;
    }

    public Vector3f getLightColor() {
        return lightColor;
    }

    public Vector3f getAmbientColor() {
        return ambientColor;
    }

    public Vector3f getSkyColor() {
        return skyColor;
    }

    public String getTimeString() {
        // Format as HH:MM
        int totalMinutes = (int) (timeOfDay * 24.0f * 60.0f);
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        String phase = (hours >= 6 && hours < 18) ? "Day" : "Night";
        return String.format("%02d:%02d (%s)", hours, minutes, phase);
    }
}

