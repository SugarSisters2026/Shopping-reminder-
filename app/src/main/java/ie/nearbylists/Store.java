package ie.nearbylists;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Keeps the places and their lists on the phone. Nothing leaves the device. */
final class Store {

    static final class Item {
        String text;
        boolean done;
    }

    static final class Place {
        String id = UUID.randomUUID().toString();
        String name = "";
        double lat;
        double lng;
        float radius = 300f;
        long lastAlert;
        final List<Item> items = new ArrayList<>();

        List<String> outstanding() {
            List<String> out = new ArrayList<>();
            for (Item item : items) {
                if (!item.done) {
                    out.add(item.text);
                }
            }
            return out;
        }
    }

    private static final String FILE = "nearby_lists";
    private static final String KEY = "places";

    private Store() {
    }

    static synchronized List<Place> load(Context context) {
        List<Place> places = new ArrayList<>();
        String json = prefs(context).getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Place place = new Place();
                place.id = o.getString("id");
                place.name = o.getString("name");
                place.lat = o.getDouble("lat");
                place.lng = o.getDouble("lng");
                place.radius = (float) o.optDouble("radius", 300d);
                place.lastAlert = o.optLong("lastAlert", 0L);
                JSONArray items = o.optJSONArray("items");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject itemJson = items.getJSONObject(j);
                        Item item = new Item();
                        item.text = itemJson.getString("text");
                        item.done = itemJson.optBoolean("done", false);
                        place.items.add(item);
                    }
                }
                places.add(place);
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return places;
    }

    static synchronized void save(Context context, List<Place> places) {
        JSONArray array = new JSONArray();
        try {
            for (Place place : places) {
                JSONObject o = new JSONObject();
                o.put("id", place.id);
                o.put("name", place.name);
                o.put("lat", place.lat);
                o.put("lng", place.lng);
                o.put("radius", (double) place.radius);
                o.put("lastAlert", place.lastAlert);
                JSONArray items = new JSONArray();
                for (Item item : place.items) {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("text", item.text);
                    itemJson.put("done", item.done);
                    items.put(itemJson);
                }
                o.put("items", items);
                array.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs(context).edit().putString(KEY, array.toString()).commit();
    }

    static Place find(List<Place> places, String id) {
        for (Place place : places) {
            if (place.id.equals(id)) {
                return place;
            }
        }
        return null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
