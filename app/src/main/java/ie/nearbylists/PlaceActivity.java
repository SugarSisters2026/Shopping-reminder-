package ie.nearbylists;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One place and its list: add, tick and remove things. */
public class PlaceActivity extends Activity {

    static final String EXTRA_ID = "place_id";

    private static final int[] DISTANCES = {150, 300, 500, 1000};

    private String placeId;
    private EditText input;
    private LinearLayout itemsBox;
    private Button distanceButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        placeId = getIntent().getStringExtra(EXTRA_ID);
        Store.Place place = current();
        if (place == null) {
            finish();
            return;
        }
        setTitle(place.name);

        LinearLayout root = Ui.column(this, 16);

        LinearLayout addRow = Ui.row(this);
        input = new EditText(this);
        input.setHint("Add something");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((view, action, event) -> {
            addFromInput();
            return true;
        });
        addRow.addView(input, Ui.weight());
        Button add = Ui.button(this, "Add");
        add.setOnClickListener(v -> addFromInput());
        addRow.addView(add);
        root.addView(addRow);

        TextView tip = Ui.text(this, "Put commas between things to add several at once.", 14);
        tip.setTextColor(Color.GRAY);
        root.addView(tip);

        itemsBox = new LinearLayout(this);
        itemsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemsBox, Ui.fill(this, 12));

        Button clear = Ui.button(this, "Remove everything ticked");
        clear.setOnClickListener(v -> change(p -> {
            List<Store.Item> keep = new ArrayList<>();
            for (Store.Item item : p.items) {
                if (!item.done) {
                    keep.add(item);
                }
            }
            p.items.clear();
            p.items.addAll(keep);
        }));
        root.addView(clear, Ui.fill(this, 24));

        distanceButton = Ui.button(this, "");
        distanceButton.setOnClickListener(v -> nextDistance());
        root.addView(distanceButton, Ui.fill(this, 8));

        Button test = Ui.button(this, "Send a test alert");
        test.setOnClickListener(v -> Alerts.show(this, placeId, true));
        root.addView(test, Ui.fill(this, 8));

        Button delete = Ui.button(this, "Delete this place");
        delete.setOnClickListener(v -> confirmDelete());
        root.addView(delete, Ui.fill(this, 8));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        render();
    }

    private Store.Place current() {
        return Store.find(Store.load(this), placeId);
    }

    private void change(Consumer<Store.Place> edit) {
        List<Store.Place> places = Store.load(this);
        Store.Place place = Store.find(places, placeId);
        if (place == null) {
            return;
        }
        edit.accept(place);
        Store.save(this, places);
        itemsBox.post(this::render);
    }

    private void addFromInput() {
        final String typed = input.getText().toString();
        input.setText("");
        change(place -> {
            for (String part : typed.split("[,\\n]")) {
                String text = part.trim();
                if (text.isEmpty()) {
                    continue;
                }
                boolean already = false;
                for (Store.Item item : place.items) {
                    if (item.text.equalsIgnoreCase(text)) {
                        item.done = false;
                        already = true;
                    }
                }
                if (!already) {
                    Store.Item item = new Store.Item();
                    item.text = text;
                    place.items.add(item);
                }
            }
        });
    }

    private void render() {
        Store.Place place = current();
        if (place == null) {
            finish();
            return;
        }
        distanceButton.setText("Alert me within " + Math.round(place.radius) + " metres");
        itemsBox.removeAllViews();
        if (place.items.isEmpty()) {
            itemsBox.addView(Ui.text(this, "Nothing on this list yet.", 18));
            return;
        }
        for (int i = 0; i < place.items.size(); i++) {
            final int index = i;
            Store.Item item = place.items.get(i);
            LinearLayout row = Ui.row(this);

            CheckBox box = new CheckBox(this);
            box.setText(item.text);
            box.setTextSize(19);
            box.setChecked(item.done);
            if (item.done) {
                box.setPaintFlags(box.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            box.setOnCheckedChangeListener((view, checked) -> change(p -> {
                if (index < p.items.size()) {
                    p.items.get(index).done = checked;
                }
            }));
            row.addView(box, Ui.weight());

            Button remove = Ui.button(this, "Remove");
            remove.setOnClickListener(v -> change(p -> {
                if (index < p.items.size()) {
                    p.items.remove(index);
                }
            }));
            row.addView(remove);

            itemsBox.addView(row, Ui.fill(this, 4));
        }
    }

    private void nextDistance() {
        Store.Place place = current();
        if (place == null) {
            return;
        }
        int next = DISTANCES[0];
        for (int distance : DISTANCES) {
            if (distance > Math.round(place.radius)) {
                next = distance;
                break;
            }
        }
        final float radius = next;
        change(p -> p.radius = radius);
        Geofences.sync(this, null);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage("Delete this place and its list?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<Store.Place> places = Store.load(this);
                    Store.Place place = Store.find(places, placeId);
                    if (place != null) {
                        places.remove(place);
                        Store.save(this, places);
                    }
                    Geofences.sync(this, null);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
