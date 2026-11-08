package ie.nearbylists;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** The first screen: your places, and setting the app up. */
public class MainActivity extends Activity {

    private LinearLayout setupBox;
    private TextView setupText;
    private Button setupButton;
    private LinearLayout placesBox;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = Ui.column(this, 16);

        setupBox = Ui.column(this, 12);
        setupBox.setBackgroundColor(Color.parseColor("#FFF3CD"));
        setupText = Ui.text(this, "", 17);
        setupButton = Ui.button(this, "Allow");
        setupBox.addView(setupText);
        setupBox.addView(setupButton, Ui.fill(this, 8));
        root.addView(setupBox);

        placesBox = new LinearLayout(this);
        placesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(placesBox, Ui.fill(this, 8));

        Button here = Ui.button(this, "Add where I am now");
        here.setOnClickListener(v -> addHere());
        root.addView(here, Ui.fill(this, 20));

        Button search = Ui.button(this, "Search for a place");
        search.setOnClickListener(v -> askSearch());
        root.addView(search, Ui.fill(this, 8));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        refresh();
    }

    private void refresh() {
        refreshSetup();
        renderPlaces();
        Geofences.sync(this, null);
    }

    private boolean granted(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private String nextPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            return Manifest.permission.POST_NOTIFICATIONS;
        }
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return Manifest.permission.ACCESS_FINE_LOCATION;
        }
        if (!granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            return Manifest.permission.ACCESS_BACKGROUND_LOCATION;
        }
        return null;
    }

    private void refreshSetup() {
        final String permission = nextPermission();
        if (permission == null) {
            setupBox.setVisibility(View.GONE);
            return;
        }
        setupBox.setVisibility(View.VISIBLE);
        if (permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
            setupText.setText("Step 1 of 3. Allow notifications so the app can show your list.");
        } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            setupText.setText("Step 2 of 3. Allow location and choose Precise.");
        } else {
            setupText.setText("Step 3 of 3. Choose \"Allow all the time\". "
                    + "Without it, alerts stop when the app is closed.");
        }
        setupButton.setOnClickListener(v -> ask(permission));
    }

    private void ask(String permission) {
        SharedPreferences prefs = getSharedPreferences("setup", MODE_PRIVATE);
        boolean askedBefore = prefs.getBoolean(permission, false);
        if (askedBefore && (!shouldShowRequestPermissionRationale(permission)
                || permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION))) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
            return;
        }
        prefs.edit().putBoolean(permission, true).apply();
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            requestPermissions(new String[]{permission}, 1);
        }
    }

    private void renderPlaces() {
        placesBox.removeAllViews();
        List<Store.Place> places = Store.load(this);
        if (places.isEmpty()) {
            placesBox.addView(Ui.text(this,
                    "No places yet. Add your first one below.", 18));
            return;
        }
        for (final Store.Place place : places) {
            LinearLayout card = Ui.column(this, 14);
            card.setBackgroundColor(Color.parseColor("#E8F2EF"));
            card.addView(Ui.text(this, place.name, 21));
            int count = place.outstanding().size();
            card.addView(Ui.text(this,
                    count + (count == 1 ? " thing to get" : " things to get"), 15));
            card.setOnClickListener(v -> openPlace(place.id));
            placesBox.addView(card, Ui.fill(this, 8));
        }
    }

    private void openPlace(String id) {
        startActivity(new Intent(this, PlaceActivity.class).putExtra(PlaceActivity.EXTRA_ID, id));
    }

    @SuppressLint("MissingPermission")
    private void addHere() {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Allow location first, using the button at the top",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Finding where you are", Toast.LENGTH_SHORT).show();
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Could not get your location. Is location turned on?",
                                Toast.LENGTH_LONG).show();
                    } else {
                        askName("", location.getLatitude(), location.getLongitude());
                    }
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not get your location", Toast.LENGTH_LONG).show());
    }

    private EditText input(String value, String hint) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edit.setHint(hint);
        edit.setText(value);
        return edit;
    }

    private void askSearch() {
        final EditText edit = input("", "For example B&Q Naas");
        new AlertDialog.Builder(this)
                .setTitle("Search for a place")
                .setView(edit)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = edit.getText().toString().trim();
                    if (!query.isEmpty()) {
                        search(query);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void search(final String query) {
        Toast.makeText(this, "Searching", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<Address> found = null;
            try {
                found = new Geocoder(this, Locale.getDefault())
                        .getFromLocationName(query + ", Ireland", 8);
            } catch (IOException | IllegalArgumentException e) {
                found = null;
            }
            final List<Address> results = found;
            runOnUiThread(() -> showResults(query, results));
        }).start();
    }

    private void showResults(final String query, final List<Address> results) {
        if (isFinishing()) {
            return;
        }
        if (results == null || results.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("Nothing found for " + query
                            + ". Try the town or the address, or use Add where I am now "
                            + "when you are there.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] labels = new String[results.size()];
        for (int i = 0; i < labels.length; i++) {
            String line = results.get(i).getAddressLine(0);
            labels[i] = line != null ? line : query;
        }
        new AlertDialog.Builder(this)
                .setTitle("Which one?")
                .setItems(labels, (dialog, which) -> {
                    Address address = results.get(which);
                    askName(query, address.getLatitude(), address.getLongitude());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askName(String suggested, final double lat, final double lng) {
        final EditText edit = input(suggested, "Name of this place");
        new AlertDialog.Builder(this)
                .setTitle("Name this place")
                .setView(edit)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = edit.getText().toString().trim();
                    savePlace(name.isEmpty() ? "My place" : name, lat, lng);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePlace(String name, double lat, double lng) {
        List<Store.Place> places = Store.load(this);
        Store.Place place = new Store.Place();
        place.name = name;
        place.lat = lat;
        place.lng = lng;
        places.add(place);
        Store.save(this, places);
        Geofences.sync(this, null);
        openPlace(place.id);
    }
}
