package com.glide.taxi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(19, 45, 40);
    private static final int GREEN = Color.rgb(8, 127, 105);
    private static final int MUTED = Color.rgb(101, 119, 111);
    private static final int BG = Color.rgb(247, 249, 248);
    private static final int MINT = Color.rgb(220, 244, 231);
    private static final int WHITE = Color.WHITE;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RideStore store;
    private RideEngine.Place destination;
    private RideEngine.Service service = RideEngine.Service.EVERYDAY;
    private long scheduledAt;
    private String tab = "Ride";
    private LinearLayout root;
    private LinearLayout content;
    private boolean foreground;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!foreground) return;
            if (store.active != null && RideEngine.tick(store.active, System.currentTimeMillis())) {
                RideEngine.Trip trip = store.active;
                store.save();
                if (trip.stage == RideEngine.Stage.COMPLETED) {
                    store.archive();
                    render();
                    receipt(trip);
                } else render();
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new RideStore(this);
        if (state != null) {
            String savedDestination = state.getString("destination");
            if (savedDestination != null) destination = RideEngine.place(savedDestination);
            service = RideEngine.Service.valueOf(state.getString("service", "EVERYDAY"));
            scheduledAt = state.getLong("schedule", 0);
            tab = state.getString("tab", "Ride");
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (destination != null) state.putString("destination", destination.id());
        state.putString("service", service.name());
        state.putLong("schedule", scheduledAt);
        state.putString("tab", tab);
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        handler.post(ticker);
    }

    @Override protected void onPause() {
        foreground = false;
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void render() {
        root = column();
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root);
        root.requestApplyInsets();
        LinearLayout header = row();
        pad(header, 24, 12, 24, 12);
        TextView logo = text("glide", 32, INK, true);
        logo.setLetterSpacing(-0.06f);
        header.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("LOCAL DEMO", 10, GREEN, true);
        badge.setLetterSpacing(0.1f);
        pad(badge, 12, 8, 12, 8);
        badge.setBackground(shape(MINT, 20, 0));
        header.addView(badge);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        pad(content, 22, 8, 22, 20);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        switch (tab) {
            case "Activity" -> history();
            case "Account" -> account();
            default -> {
                if (store.active != null) activeRide();
                else if (destination != null) chooseRide();
                else home();
            }
        }
        navigation();
    }

    private void home() {
        label(content, "YOUR CITY. YOUR PACE.", 11, GREEN, true);
        gap(content, 7);
        label(content, "Good things are\njust a ride away.", 32, INK, true);
        gap(content, 8);
        label(content, "A little less waiting. A lot more going.", 14, MUTED, false);
        gap(content, 20);

        LinearLayout search = row();
        search.setBackground(shape(WHITE, 18, 0xFFE0E7E2));
        pad(search, 16, 16, 16, 16);
        search.addView(icon("pin", GREEN, 26));
        TextView prompt = text("Where to?", 21, INK, true);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(0, -2, 1);
        promptParams.setMargins(dp(12), 0, 0, 0);
        search.addView(prompt, promptParams);
        TextView arrow = text(">", 24, GREEN, false);
        search.addView(arrow);
        search.setContentDescription("Where to? Search demo destinations");
        search.setOnClickListener(v -> searchDestination());
        content.addView(search);
        gap(content, 12);

        LinearLayout shortcuts = row();
        shortcut(shortcuts, "home", "Home", RideEngine.place(store.homeId).name(), () -> selectDestination(store.homeId));
        View space = new View(this); shortcuts.addView(space, new LinearLayout.LayoutParams(dp(10), 1));
        shortcut(shortcuts, "work", "Work", RideEngine.place(store.workId).name(), () -> selectDestination(store.workId));
        content.addView(shortcuts);
        gap(content, 20);
        LinearLayout map = column();
        map.setBackground(shape(WHITE, 20, 0));
        map.setClipToOutline(true);
        map.addView(new CityMapView(this, pickup().name(), null, false),
            new LinearLayout.LayoutParams(-1, dp(235)));
        LinearLayout mapFooter = row();
        pad(mapFooter, 14, 12, 14, 12);
        mapFooter.addView(icon("pin", GREEN, 18));
        TextView current = text("Pickup: " + pickup().name(), 13, INK, true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
        cp.setMargins(dp(8), 0, 0, 0); mapFooter.addView(current, cp);
        TextView change = text("Change", 12, GREEN, true);
        change.setMinHeight(dp(48)); change.setGravity(Gravity.CENTER);
        change.setOnClickListener(v -> choosePickup());
        mapFooter.addView(change);
        map.addView(mapFooter);
        content.addView(map);
        gap(content, 18);

        LinearLayout promo = row();
        promo.setBackground(shape(MINT, 18, 0));
        pad(promo, 16, 16, 16, 16);
        LinearLayout copy = column();
        label(copy, "Take the scenic route.", 17, INK, true);
        gap(copy, 5);
        label(copy, "Book now or plan a little ahead.", 12, MUTED, false);
        promo.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        promo.addView(icon("car", GREEN, 56));
        promo.setOnClickListener(v -> searchDestination());
        content.addView(promo);
        gap(content, 14);
        label(content, "Built for a test drive. No real rides or charges.", 11, MUTED, false);
    }

    private void shortcut(LinearLayout parent, String symbol, String title, String subtitle, Runnable action) {
        LinearLayout card = row();
        pad(card, 12, 12, 10, 12);
        card.setBackground(shape(WHITE, 15, 0xFFE6EBE7));
        card.addView(icon(symbol, GREEN, 23));
        LinearLayout words = column();
        pad(words, 9, 0, 0, 0);
        label(words, title, 14, INK, true);
        TextView sub = text(subtitle, 10, MUTED, false);
        sub.setSingleLine(true); sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        words.addView(sub);
        card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(card, new LinearLayout.LayoutParams(0, dp(70), 1));
        card.setOnClickListener(v -> action.run());
        card.setContentDescription(title + ": " + subtitle);
    }

    private void chooseRide() {
        TextView back = text("<  Change destination", 14, GREEN, true);
        back.setMinHeight(dp(48)); back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> searchDestination());
        content.addView(back);
        label(content, "Make it your ride.", 28, INK, true);
        gap(content, 5);
        RideEngine.Quote quote = RideEngine.quote(pickup(), destination, service);
        label(content, String.format(Locale.US, "%.1f km  /  about %d min  /  estimated", quote.kilometers(), quote.minutes()),
            12, MUTED, false);
        gap(content, 14);
        map(155, false);
        gap(content, 14);
        LinearLayout route = card();
        detail(route, "FROM", pickup().name(), "Change", this::choosePickup);
        divider(route);
        detail(route, "TO", destination.name(), "Edit", this::searchDestination);
        content.addView(route);
        gap(content, 16);
        for (RideEngine.Service option : RideEngine.Service.values()) {
            LinearLayout ride = row();
            pad(ride, 12, 11, 12, 11);
            boolean selected = service == option;
            ride.setBackground(shape(selected ? MINT : WHITE, 16, selected ? GREEN : 0xFFE3EAE5));
            ride.addView(icon("car", selected ? GREEN : MUTED, 48));
            LinearLayout info = column();
            pad(info, 12, 0, 5, 0);
            label(info, option.title + "  /  " + option.seats, 17, INK, true);
            label(info, option.arrivalMinutes + " min away", 12, GREEN, true);
            label(info, option.description, 10, MUTED, false);
            ride.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            String price = RideEngine.money(RideEngine.quote(pickup(), destination, option).totalCents());
            ride.addView(text(price, 19, INK, true));
            ride.setContentDescription(option.title + ", " + option.seats + " seats, " + price
                + ", " + (selected ? "selected" : "tap to select"));
            ride.setOnClickListener(v -> { service = option; render(); });
            content.addView(ride);
            gap(content, 8);
        }
        gap(content, 5);
        LinearLayout preferences = card();
        detail(preferences, "PAYMENT", paymentLabel(), "Change", () -> choosePayment(this::render));
        divider(preferences);
        detail(preferences, "PICKUP TIME", scheduledAt == 0 ? "Now" : date(scheduledAt),
            "Change", this::schedule);
        content.addView(preferences);
        gap(content, 10);
        TextView breakdown = text("View estimated fare breakdown", 12, GREEN, true);
        breakdown.setMinHeight(dp(44)); breakdown.setGravity(Gravity.CENTER);
        breakdown.setOnClickListener(v -> fareBreakdown(quote));
        content.addView(breakdown);
        button(content, (scheduledAt == 0 ? "Confirm " : "Schedule ") + service.title + "  -  "
            + RideEngine.money(quote.totalCents()), () -> {
                try {
                    store.active = RideEngine.book(pickup(), destination, service, store.payment,
                        scheduledAt, System.currentTimeMillis());
                    store.save();
                    destination = null;
                    scheduledAt = 0;
                    render();
                } catch (IllegalArgumentException ex) { message("Check your ride", ex.getMessage()); }
            }, true);
        gap(content, 8);
        label(content, "Demo estimate in USD. No payment is collected.", 11, MUTED, false);
    }

    private void activeRide() {
        RideEngine.Trip trip = store.active;
        destination = null;
        String title = switch (trip.stage) {
            case SCHEDULED -> "Consider it planned.";
            case SEARCHING -> "Finding your ride.";
            case ARRIVING -> "Alex is on the way.";
            default -> "Enjoy the journey.";
        };
        label(content, "YOUR DEMO RIDE", 11, GREEN, true);
        gap(content, 8);
        label(content, title, 29, INK, true);
        gap(content, 7);
        label(content, switch (trip.stage) {
            case SCHEDULED -> date(trip.scheduledAt);
            case SEARCHING -> "Matching with a simulated nearby driver...";
            case ARRIVING -> "Your simulated driver will arrive shortly.";
            default -> "On the way to " + trip.destination().name();
        }, 13, MUTED, false);
        gap(content, 18);
        content.addView(new CityMapView(this, trip.pickup().name(), trip.destination().name(),
            trip.stage == RideEngine.Stage.IN_PROGRESS), new LinearLayout.LayoutParams(-1, dp(235)));
        gap(content, 16);
        if (trip.stage == RideEngine.Stage.ARRIVING || trip.stage == RideEngine.Stage.IN_PROGRESS) {
            LinearLayout driver = card();
            LinearLayout row = row();
            TextView avatar = text("AL", 22, GREEN, true);
            avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(MINT, 40, 0));
            row.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));
            LinearLayout info = column();
            pad(info, 12, 0, 0, 0);
            label(info, "Alex Lee  /  4.9 stars", 17, INK, true);
            label(info, "White Toyota Prius", 13, MUTED, false);
            label(info, "DEMO 204  /  Simulated driver", 11, GREEN, true);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            driver.addView(row);
            content.addView(driver);
            gap(content, 12);
        }
        LinearLayout details = card();
        detail(details, "PICKUP", trip.pickup().name(), null, null);
        divider(details);
        detail(details, "DESTINATION", trip.destination().name(), null, null);
        divider(details);
        detail(details, trip.service.title.toUpperCase(Locale.US),
            RideEngine.money(trip.fareCents) + "  /  " + trip.payment, null, null);
        content.addView(details);
        gap(content, 14);
        button(content, switch (trip.stage) {
            case SCHEDULED -> "Start scheduled demo now";
            case SEARCHING -> "Simulate driver match";
            case ARRIVING -> "Simulate pickup";
            default -> "Complete demo ride";
        }, this::advanceDemo, true);
        gap(content, 8);
        button(content, "Safety & trip sharing", () -> safety(trip), false);
        if (trip.stage != RideEngine.Stage.IN_PROGRESS) {
            gap(content, 8);
            button(content, "Cancel ride", () -> new AlertDialog.Builder(this)
                .setTitle("Cancel this demo ride?")
                .setMessage("There is no cancellation fee.")
                .setNegativeButton("Keep ride", null)
                .setPositiveButton("Cancel ride", (d, w) -> {
                    try {
                        RideEngine.cancel(trip, System.currentTimeMillis());
                        store.archive();
                        render();
                    } catch (IllegalStateException ex) {
                        message("Ride has already started", "This demo ride can no longer be cancelled. "
                            + "You can complete it from the Ride tab.");
                    }
                }).show(), false);
        }
        gap(content, 12);
        label(content, "Simulation only. Stages advance automatically while open.\nUse the buttons to try the full journey faster.", 11, MUTED, false);
    }

    private void advanceDemo() {
        if (store.active == null) return;
        RideEngine.Trip trip = store.active;
        RideEngine.advance(trip, System.currentTimeMillis());
        store.save();
        if (trip.stage == RideEngine.Stage.COMPLETED) {
            store.archive();
            render();
            receipt(trip);
        } else render();
    }

    private void history() {
        label(content, "The places you've been.", 28, INK, true);
        gap(content, 7);
        label(content, "Your rides, receipts, and little adventures.", 13, MUTED, false);
        gap(content, 22);
        if (store.active != null) {
            button(content, "View active " + (store.active.stage == RideEngine.Stage.SCHEDULED
                ? "scheduled ride" : "ride"), () -> { tab = "Ride"; render(); }, true);
            gap(content, 16);
        }
        if (store.history.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(icon("car", GREEN, 64));
            gap(empty, 15);
            label(empty, "Your first ride awaits.", 22, INK, true);
            gap(empty, 8);
            label(empty, "Completed and cancelled demo rides will appear here.", 14, MUTED, false);
            gap(empty, 20);
            button(empty, "Let's go somewhere", () -> { tab = "Ride"; render(); }, true);
            content.addView(empty);
        }
        for (RideEngine.Trip trip : store.history) {
            LinearLayout item = card();
            label(item, trip.destination().name(), 19, INK, true);
            gap(item, 5);
            label(item, date(trip.createdAt), 12, MUTED, false);
            gap(item, 10);
            label(item, trip.pickup().name() + "  >  " + trip.destination().name(), 12, MUTED, false);
            gap(item, 10);
            label(item, trip.stage == RideEngine.Stage.CANCELLED ? "Cancelled  /  No charge"
                : RideEngine.money(trip.fareCents) + "  /  Completed"
                    + (trip.rating > 0 ? "  /  " + trip.rating + " stars" : ""), 14, GREEN, true);
            if (trip.stage == RideEngine.Stage.COMPLETED) {
                item.setOnClickListener(v -> receipt(trip));
                label(item, "Tap for receipt & rating", 11, MUTED, false);
            }
            content.addView(item);
            gap(content, 12);
        }
    }

    private void account() {
        label(content, "A space that's yours.", 28, INK, true);
        gap(content, 8);
        label(content, "Demo rider  /  No sign-in required", 14, MUTED, false);
        gap(content, 22);
        LinearLayout saved = card();
        label(saved, "Saved places", 19, INK, true);
        gap(saved, 12);
        detail(saved, "HOME", RideEngine.place(store.homeId).name(), "Edit", () ->
            pickPlace("Set your home", place -> { store.homeId = place.id(); store.save(); render(); }));
        divider(saved);
        detail(saved, "WORK", RideEngine.place(store.workId).name(), "Edit", () ->
            pickPlace("Set your work", place -> { store.workId = place.id(); store.save(); render(); }));
        content.addView(saved);
        gap(content, 14);
        LinearLayout payment = card();
        detail(payment, "DEFAULT PAYMENT", paymentLabel(), "Change", () -> choosePayment(this::render));
        content.addView(payment);
        gap(content, 18);
        button(content, "Help & demo guide", () -> message("Welcome to Glide",
            "1. Choose one of eight San Francisco demo destinations.\n\n"
            + "2. Compare three ride options, check the fare, and book now or schedule ahead.\n\n"
            + "3. Follow the simulated driver, complete your ride, and leave a rating.\n\n"
            + "The map is an illustration, not GPS navigation. Drivers, trip progress, fares, and card payments are simulated. "
            + "Cash is a demo selection too. No real transport, charges, live support, or emergency monitoring is provided."), false);
        gap(content, 10);
        button(content, "Privacy & local data", () -> message("Your demo stays on this device",
            "Glide requires no account, location permission, or network access. Saved places, payment preference, "
            + "active rides, receipts, and ratings are stored in the app's private local storage. "
            + "The app never asks for real payment details. Sharing a trip opens Android's share sheet only when you choose it. "
            + "Remove all local app data using the button below or Android Settings."), false);
        gap(content, 10);
        button(content, "Clear local demo data", () -> new AlertDialog.Builder(this)
            .setTitle("Clear all demo data?")
            .setMessage("This removes active rides, history, ratings, and saved preferences on this device.")
            .setNegativeButton("Keep data", null)
            .setPositiveButton("Clear data", (d, w) -> {
                getSharedPreferences("glide_rides", MODE_PRIVATE).edit().clear().apply();
                store = new RideStore(this);
                destination = null;
                scheduledAt = 0;
                service = RideEngine.Service.EVERYDAY;
                render();
                toast("Local demo data cleared");
            }).show(), false);
        gap(content, 24);
        label(content, "GLIDE  /  ANDROID RIDER DEMO  /  1.0", 10, GREEN, true);
    }

    private void navigation() {
        LinearLayout nav = row();
        pad(nav, 14, 7, 14, 7);
        nav.setBackgroundColor(WHITE);
        String[] tabs = { "Ride", "Activity", "Account" };
        String[] icons = { "car", "clock", "person" };
        for (int i = 0; i < tabs.length; i++) {
            String title = tabs[i];
            boolean selected = tab.equals(title);
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setBackground(shape(selected ? MINT : WHITE, 16, 0));
            item.addView(icon(icons[i], selected ? GREEN : MUTED, 24));
            gap(item, 3);
            TextView navLabel = text(title, 11, selected ? GREEN : MUTED, selected);
            navLabel.setGravity(Gravity.CENTER);
            item.addView(navLabel);
            item.setContentDescription(title + " tab");
            item.setSelected(selected);
            item.setOnClickListener(v -> { tab = title; render(); });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(nav);
    }

    private void searchDestination() {
        LinearLayout body = column();
        pad(body, 20, 0, 20, 12);
        EditText search = new EditText(this);
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setHint("Search San Francisco demo places");
        search.setContentDescription("Search destinations");
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(56)));
        label(body, "8 demo places. No live address search.", 11, MUTED, false);
        ScrollView scroll = new ScrollView(this);
        LinearLayout results = column();
        scroll.addView(results);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, dp(340)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Where are you headed?")
            .setView(body).setNegativeButton("Close", null).create();
        Runnable filter = () -> {
            results.removeAllViews();
            String query = search.getText().toString().trim().toLowerCase(Locale.US);
            for (RideEngine.Place place : RideEngine.PLACES) {
                if (place.id().equals(pickup().id())) continue;
                if (!(place.name() + " " + place.address()).toLowerCase(Locale.US).contains(query)) continue;
                LinearLayout result = column();
                pad(result, 0, 14, 0, 14);
                label(result, place.name(), 16, INK, true);
                label(result, place.address(), 12, MUTED, false);
                result.setOnClickListener(v -> {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(search.getWindowToken(), 0);
                    dialog.dismiss();
                    selectDestination(place.id());
                });
                results.addView(result);
                divider(results);
            }
            if (results.getChildCount() == 0) {
                gap(results, 22);
                label(results, "No demo places found.\nTry Airport, Union Square, or Pier.", 14, MUTED, false);
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        filter.run();
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void selectDestination(String id) {
        if (pickup().id().equals(id)) {
            message("You're already there", "Choose a destination different from your pickup.");
            return;
        }
        destination = RideEngine.place(id);
        tab = "Ride";
        render();
    }

    private void choosePickup() {
        pickPlace("Choose demo pickup", place -> {
            if (destination != null && destination.id().equals(place.id())) {
                message("Choose another pickup", "Pickup and destination must be different.");
                return;
            }
            store.pickupId = place.id();
            store.save();
            render();
        });
    }

    private void pickPlace(String title, Consumer<RideEngine.Place> selected) {
        String[] names = RideEngine.PLACES.stream().map(p -> p.name() + "\n" + p.address()).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle(title).setItems(names, (d, which) ->
            selected.accept(RideEngine.PLACES.get(which))).setNegativeButton("Close", null).show();
    }

    private void choosePayment(Runnable done) {
        new AlertDialog.Builder(this).setTitle("Demo payment method")
            .setSingleChoiceItems(new String[] { "Demo card  /  No real charges", "Cash  /  Simulated only" },
                store.payment.equals("Cash") ? 1 : 0, (dialog, index) -> {
                    store.payment = index == 0 ? "Demo card" : "Cash";
                    store.save();
                    dialog.dismiss();
                    done.run();
                }).setNegativeButton("Close", null).show();
    }

    private void schedule() {
        new AlertDialog.Builder(this).setTitle("When do you want to go?")
            .setItems(new String[] { "Ride now", "Schedule for later" }, (dialog, which) -> {
                if (which == 0) { scheduledAt = 0; render(); return; }
                Calendar chosen = Calendar.getInstance();
                chosen.setTimeInMillis(scheduledAt == 0 ? System.currentTimeMillis() + 3_600_000 : scheduledAt);
                DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
                    chosen.set(year, month, day);
                    new TimePickerDialog(this, (timeView, hour, minute) -> {
                        chosen.set(Calendar.HOUR_OF_DAY, hour);
                        chosen.set(Calendar.MINUTE, minute);
                        chosen.set(Calendar.SECOND, 0);
                        chosen.set(Calendar.MILLISECOND, 0);
                        long candidate = chosen.getTimeInMillis();
                        long now = System.currentTimeMillis();
                        if (candidate < now + 300_000 || candidate > now + 30L * 24 * 60 * 60_000) {
                            message("Choose another time", "Choose a time at least 5 minutes ahead and within 30 days.");
                            return;
                        }
                        scheduledAt = candidate;
                        render();
                    }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), false).show();
                }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH));
                picker.getDatePicker().setMinDate(System.currentTimeMillis());
                picker.getDatePicker().setMaxDate(System.currentTimeMillis() + 30L * 24 * 60 * 60_000);
                picker.show();
            }).setNegativeButton("Close", null).show();
    }

    private void fareBreakdown(RideEngine.Quote quote) {
        message("Your estimated fare", "Base fare                         " + RideEngine.money(quote.baseCents())
            + "\nDistance                           " + RideEngine.money(quote.distanceCents())
            + "\nTime                                  " + RideEngine.money(quote.timeCents())
            + "\nBooking fee                     " + RideEngine.money(quote.bookingCents())
            + "\n\nDemo total                       " + RideEngine.money(quote.totalCents())
            + "\n\nIllustrative USD pricing, not a real quote. No extra charges or payment will occur.");
    }

    private void receipt(RideEngine.Trip trip) {
        LinearLayout body = column();
        pad(body, 24, 5, 24, 15);
        label(body, trip.destination().name(), 23, INK, true);
        gap(body, 6);
        label(body, trip.pickup().name() + "  >  " + trip.destination().name(), 13, MUTED, false);
        gap(body, 12);
        label(body, RideEngine.money(trip.fareCents), 36, GREEN, true);
        label(body, trip.service.title + "  /  " + trip.payment + "  /  No real charge", 12, MUTED, false);
        gap(body, 8);
        label(body, date(trip.createdAt) + "\nReceipt " + trip.id, 12, MUTED, false);
        gap(body, 18);
        label(body, "How was your demo ride?", 17, INK, true);
        RatingBar rating = new RatingBar(this);
        rating.setNumStars(5);
        rating.setStepSize(1);
        rating.setRating(trip.rating);
        rating.setContentDescription("Rate demo ride from 1 to 5 stars");
        body.addView(rating, new LinearLayout.LayoutParams(-2, -2));
        new AlertDialog.Builder(this).setTitle("You've arrived.")
            .setView(body).setNegativeButton("Close", null)
            .setPositiveButton("Save rating", (dialog, which) -> {
                int value = Math.round(rating.getRating());
                if (value < 1) { toast("No rating selected. You can rate this ride in Activity."); return; }
                trip.rating = value;
                store.save();
                render();
                toast("Thanks for rating your ride");
            }).show();
    }

    private void safety(RideEngine.Trip trip) {
        new AlertDialog.Builder(this).setTitle("Safety, without the guesswork")
            .setMessage("Demo driver: Alex Lee\nWhite Toyota Prius / DEMO 204\n\n"
                + "In a real ride, always check your vehicle, plate, and driver before getting in.\n\n"
                + "This is a simulation, not an emergency service. No driver or safety team is monitoring this trip. "
                + "For a real emergency, contact your local emergency services directly.")
            .setPositiveButton("Share demo trip", (dialog, which) -> {
                Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "Glide SIMULATED trip " + trip.id + "\n"
                    + trip.pickup().name() + " to " + trip.destination().name()
                    + "\nThis is a demo only. No live location or real ride.");
                startActivity(Intent.createChooser(share, "Share demo trip"));
            }).setNegativeButton("Close", null).show();
    }

    @Override public void onBackPressed() {
        if (!tab.equals("Ride")) { tab = "Ride"; render(); }
        else if (destination != null && store.active == null) { destination = null; render(); }
        else super.onBackPressed();
    }

    private RideEngine.Place pickup() { return RideEngine.place(store.pickupId); }
    private String paymentLabel() { return store.payment.equals("Cash") ? "Cash (simulated)" : "Demo card (no charge)"; }
    private String date(long time) {
        return new SimpleDateFormat("MMM d, yyyy  /  h:mm a", Locale.US).format(new Date(time));
    }

    private void map(int height, boolean moving) {
        CityMapView map = new CityMapView(this, pickup().name(), destination.name(), moving);
        map.setBackground(shape(MINT, 18, 0));
        map.setClipToOutline(true);
        content.addView(map, new LinearLayout.LayoutParams(-1, dp(height)));
    }

    private void detail(LinearLayout parent, String caption, String value, String action, Runnable onClick) {
        LinearLayout line = row();
        LinearLayout info = column();
        label(info, caption, 10, MUTED, true);
        gap(info, 4);
        label(info, value, 14, INK, true);
        line.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null) {
            TextView link = text(action, 12, GREEN, true);
            link.setGravity(Gravity.CENTER);
            pad(link, 12, 0, 0, 0);
            link.setMinHeight(dp(48));
            link.setContentDescription(action + " " + caption.toLowerCase(Locale.US));
            link.setOnClickListener(v -> onClick.run());
            line.addView(link);
        }
        parent.addView(line);
    }

    private LinearLayout card() {
        LinearLayout card = column();
        pad(card, 16, 16, 16, 16);
        card.setBackground(shape(WHITE, 18, 0xFFE4EBE6));
        return card;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private void label(LinearLayout parent, String value, int size, int color, boolean bold) {
        parent.addView(text(value, size, color, bold));
    }

    private void button(LinearLayout parent, String title, Runnable action, boolean primary) {
        TextView button = text(title, 15, primary ? WHITE : GREEN, true);
        button.setGravity(Gravity.CENTER);
        pad(button, 12, 12, 12, 12);
        button.setMinHeight(dp(52));
        button.setBackground(shape(primary ? GREEN : WHITE, 15, primary ? 0 : 0xFFDCE6DF));
        button.setOnClickListener(v -> action.run());
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,
                    android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
            }
        });
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    private void divider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(0xFFE8EDE9);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(0, dp(10), 0, dp(10));
        parent.addView(line, params);
    }

    private void gap(LinearLayout parent, int size) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private void pad(View view, int left, int top, int right, int bottom) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void message(String title, String text) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("Got it", null).show();
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    private View icon(String kind, int color, int size) {
        View view = new SymbolView(this, kind, color);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }
}
