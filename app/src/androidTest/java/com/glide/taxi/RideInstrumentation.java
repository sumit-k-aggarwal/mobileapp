package com.glide.taxi;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

public final class RideInstrumentation extends Instrumentation {
    private Activity activity;
    private int checks;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("glide_rides", 0).edit().clear().commit();
            launch();
            require(node("Where to? Search demo destinations", false) != null, "Home search visible");
            click("Where to? Search demo destinations", false);
            click("Union Square", true);
            require(node("Make it your ride.", true) != null, "Ride options visible");
            click("Glide XL, 6 seats", false);
            click("Change payment", false);
            click("Cash  /  Simulated only", true);
            click("Confirm Glide XL", false);
            require(load().active != null, "Booking persisted");
            require(load().active.service == RideEngine.Service.XL, "Selected XL stored");
            require(load().active.payment.equals("Cash"), "Payment preference stored");
            RideEngine.Trip active = load().active;
            if (active.stage == RideEngine.Stage.SEARCHING) click("Simulate driver match", true);
            click("Simulate pickup", true);
            click("Complete demo ride", true);
            click("Save rating", true);
            require(load().history.size() == 1, "Completed ride archived exactly once");
            require(load().active == null, "Active ride cleared after completion");
            click("Activity tab", true);
            require(node("Union Square", true) != null, "Completed ride appears in Activity");
            click("Union Square", true);
            // RatingBar is in a dialog window, so use the dialog's accessibility range action.
            AccessibilityNodeInfo rating = findClass(getUiAutomation().getRootInActiveWindow(), "android.widget.RatingBar");
            require(rating != null, "Receipt rating control available");
            Bundle progress = new Bundle();
            progress.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 5);
            require(rating.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(), progress),
                "Rating can be adjusted accessibly");
            click("Save rating", true);
            require(load().history.get(0).rating == 5, "Rating persisted");
            restart();
            click("Activity tab", true);
            require(node("5 stars", false) != null, "History and rating survive relaunch");

            click("Ride tab", true);
            click("Home: Pacific Heights", true);
            click("Confirm Glide", false);
            click("Cancel ride", true);
            click("Cancel ride", true);
            require(load().history.size() == 2, "Cancellation archived");
            require(load().history.get(0).stage == RideEngine.Stage.CANCELLED, "Cancelled state stored");

            click("Where to? Search demo destinations", false);
            click("San Francisco Airport", true);
            click("Change pickup time", true);
            click("Schedule for later", true);
            click("OK", true);
            click("OK", true);
            click("Schedule Glide", false);
            require(load().active.stage == RideEngine.Stage.SCHEDULED, "Scheduled through date and time pickers");
            require(load().active.scheduledAt > System.currentTimeMillis() + 300_000,
                "Selected future pickup time persisted");
            restart();
            require(node("Consider it planned.", true) != null, "Scheduled ride restored");
            require(load().active.stage == RideEngine.Stage.SCHEDULED, "Scheduled ride not started early");
            click("Start scheduled demo now", true);
            require(load().active.stage == RideEngine.Stage.SEARCHING, "Scheduled demo can be advanced");
            click("Simulate driver match", true);
            click("Cancel ride", true);
            long arrivalDeadline = SystemClock.uptimeMillis() + 20_000;
            while (load().active.stage == RideEngine.Stage.ARRIVING
                    && SystemClock.uptimeMillis() < arrivalDeadline) SystemClock.sleep(250);
            require(load().active.stage == RideEngine.Stage.IN_PROGRESS,
                "Simulation advances with cancellation confirmation open");
            click("Cancel ride", true);
            require(node("Ride has already started", true) != null, "Late cancellation handled without crashing");
            click("Got it", true);
            require(load().active.stage == RideEngine.Stage.IN_PROGRESS, "Late cancellation preserves running trip");

            click("Complete demo ride", true);
            click("Close", true);
            click("Account tab", true);
            click("Clear local demo data", true);
            click("Clear data", true);
            require(load().history.isEmpty() && load().active == null, "Clear data removes trips");
            require(load().payment.equals("Demo card"), "Clear data restores payment default");

            // Preserve a clean first-launch demo after exercising all stored state.
            runOnMainSync(() -> activity.finish());
            getTargetContext().getSharedPreferences("glide_rides", 0).edit().clear().commit();
            result.putString("stream", "\nOK: " + checks + " Android checks: booking, payment, ride lifecycle, "
                + "receipt, accessible rating, persistence, cancellation, and scheduling.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "\nFAILED after " + checks + " checks: " + failure + "\n"
                + android.util.Log.getStackTraceString(failure) + "\n"
                + describe(getUiAutomation().getRootInActiveWindow()));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private RideStore load() { return new RideStore(getTargetContext()); }

    private void launch() {
        Intent intent = new Intent(getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = startActivitySync(intent);
        waitForIdleSync();
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (getUiAutomation().getRootInActiveWindow() == null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100);
        }
        SystemClock.sleep(400);
    }

    private void restart() {
        runOnMainSync(() -> activity.finish());
        waitForIdleSync();
        launch();
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private AccessibilityNodeInfo node(String value, boolean exact) {
        return find(getUiAutomation().getRootInActiveWindow(), value, exact);
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo current, String value, boolean exact) {
        if (current == null) return null;
        String text = current.getText() == null ? "" : current.getText().toString();
        String description = current.getContentDescription() == null ? "" : current.getContentDescription().toString();
        if (exact ? value.equalsIgnoreCase(text) || value.equalsIgnoreCase(description)
                : text.toLowerCase(Locale.US).contains(value.toLowerCase(Locale.US))
                    || description.toLowerCase(Locale.US).contains(value.toLowerCase(Locale.US))) {
            return current;
        }
        for (int i = 0; i < current.getChildCount(); i++) {
            AccessibilityNodeInfo match = find(current.getChild(i), value, exact);
            if (match != null) return match;
        }
        return null;
    }

    private String describe(AccessibilityNodeInfo current) {
        if (current == null) return "";
        StringBuilder output = new StringBuilder();
        if (current.getText() != null) output.append(current.getClassName()).append(": ")
            .append(current.getText()).append('\n');
        for (int i = 0; i < current.getChildCount(); i++) output.append(describe(current.getChild(i)));
        return output.toString();
    }

    private AccessibilityNodeInfo findClass(AccessibilityNodeInfo current, String className) {
        if (current == null) return null;
        if (className.contentEquals(current.getClassName())) return current;
        for (int i = 0; i < current.getChildCount(); i++) {
            AccessibilityNodeInfo match = findClass(current.getChild(i), className);
            if (match != null) return match;
        }
        return null;
    }

    private void click(String value, boolean exact) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            waitForIdleSync();
            AccessibilityNodeInfo found = node(value, exact);
            if (found != null) {
                // Text may belong to a child of the clickable destination or ride card.
                while (found != null && !found.isClickable()) found = found.getParent();
                if (found != null && found.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    SystemClock.sleep(180);
                    waitForIdleSync();
                    return;
                }
            }
            // Scroll the current screen to reach controls on smaller test devices.
            AccessibilityNodeInfo scroll = findClass(getUiAutomation().getRootInActiveWindow(), "android.widget.ScrollView");
            if (scroll != null) scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            SystemClock.sleep(180);
        }
        throw new AssertionError("Could not click: " + value);
    }
}
