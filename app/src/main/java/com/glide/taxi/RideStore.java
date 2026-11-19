package com.glide.taxi;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class RideStore {
    private final SharedPreferences preferences;
    RideEngine.Trip active;
    final List<RideEngine.Trip> history = new ArrayList<>();
    String payment;
    String homeId;
    String workId;
    String pickupId;

    RideStore(Context context) {
        preferences = context.getSharedPreferences("glide_rides", Context.MODE_PRIVATE);
        payment = preferences.getString("payment", "Demo card");
        homeId = validPlace(preferences.getString("home", "home"), "home");
        workId = validPlace(preferences.getString("work", "work"), "work");
        pickupId = validPlace(preferences.getString("pickup", "market"), "market");
        try {
            String saved = preferences.getString("active", null);
            if (saved != null) active = decode(new JSONObject(saved));
        } catch (JSONException | IllegalArgumentException ignored) {
            active = null;
        }
        try {
            JSONArray entries = new JSONArray(preferences.getString("history", "[]"));
            for (int i = 0; i < entries.length(); i++) {
                try { history.add(decode(entries.getJSONObject(i))); }
                catch (JSONException | IllegalArgumentException ignored) { /* Skip invalid entry only. */ }
            }
        } catch (JSONException ignored) { /* Keep the app usable after invalid local storage. */ }
        if (active != null && (active.stage == RideEngine.Stage.COMPLETED
                || active.stage == RideEngine.Stage.CANCELLED)) archive();
    }

    private String validPlace(String id, String fallback) {
        try { return RideEngine.place(id).id(); }
        catch (IllegalArgumentException ex) { return fallback; }
    }

    void archive() {
        if (active == null) return;
        history.removeIf(trip -> trip.id.equals(active.id));
        history.add(0, active);
        active = null;
        save();
    }

    void save() {
        JSONArray entries = new JSONArray();
        for (RideEngine.Trip trip : history) entries.put(encode(trip));
        SharedPreferences.Editor editor = preferences.edit()
            .putString("history", entries.toString()).putString("payment", payment)
            .putString("home", homeId).putString("work", workId).putString("pickup", pickupId);
        if (active == null) editor.remove("active");
        else editor.putString("active", encode(active).toString());
        editor.apply();
    }

    private JSONObject encode(RideEngine.Trip trip) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", trip.id).put("pickup", trip.pickupId).put("destination", trip.destinationId)
                .put("service", trip.service.name()).put("payment", trip.payment)
                .put("stage", trip.stage.name()).put("fare", trip.fareCents)
                .put("created", trip.createdAt).put("scheduled", trip.scheduledAt)
                .put("stageAt", trip.stageAt).put("rating", trip.rating);
        } catch (JSONException ex) { throw new IllegalStateException(ex); }
        return json;
    }

    private RideEngine.Trip decode(JSONObject json) throws JSONException {
        RideEngine.Trip trip = new RideEngine.Trip();
        trip.id = json.getString("id");
        trip.pickupId = RideEngine.place(json.getString("pickup")).id();
        trip.destinationId = RideEngine.place(json.getString("destination")).id();
        trip.service = RideEngine.Service.valueOf(json.getString("service"));
        trip.payment = json.getString("payment");
        trip.stage = RideEngine.Stage.valueOf(json.getString("stage"));
        trip.fareCents = json.getInt("fare");
        trip.createdAt = json.getLong("created");
        trip.scheduledAt = json.getLong("scheduled");
        trip.stageAt = json.getLong("stageAt");
        trip.rating = json.optInt("rating", 0);
        return trip;
    }
}
