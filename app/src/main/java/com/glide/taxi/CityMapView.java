package com.glide.taxi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class CityMapView extends View {
    private final Paint paint = new Paint(3);
    private final Path water = new Path();
    private final Path routePath = new Path();
    private final boolean route;
    private final String origin;
    private final String destination;
    private final boolean moving;

    public CityMapView(Context context) {
        this(context, "Market Street", null, false);
    }

    CityMapView(Context context, String origin, String destination, boolean moving) {
        super(context);
        this.origin = origin;
        this.destination = destination;
        this.route = destination != null;
        this.moving = moving;
        setContentDescription(route ? "Illustrated demo route from " + origin + " to " + destination
            : "Illustrated San Francisco demo map, not live navigation");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private void color(String value) { paint.setColor(Color.parseColor(value)); }
    private void line(Canvas c, float x, float y, float x2, float y2, float width, String color) {
        color(color);
        paint.setStrokeWidth(width);
        c.drawLine(x, y, x2, y2, paint);
    }

    @Override protected void onDraw(Canvas real) {
        super.onDraw(real);
        int save = real.save();
        real.scale(getWidth() / 400f, getHeight() / 280f);
        Canvas c = real;
        c.drawColor(Color.parseColor("#EAF0EA"));
        color("#CFE5E4");
        water.reset();
        water.moveTo(330, 0); water.lineTo(400, 0); water.lineTo(400, 280);
        water.lineTo(285, 280); water.lineTo(348, 125); water.close();
        c.drawPath(water, paint);
        color("#D4E3CF");
        c.drawRoundRect(28, 35, 101, 90, 13, 13, paint);
        c.drawRoundRect(205, 182, 268, 247, 12, 12, paint);
        for (int i = -4; i < 10; i++) {
            line(c, i * 49, 0, i * 49 + 180, 280, 12, "#FFFFFF");
        }
        for (int i = -4; i < 10; i++) {
            line(c, 0, i * 46, 360, i * 46 - 130, 10, "#FFFFFF");
        }
        line(c, 290, 0, 345, 125, 18, "#FAFCF9");
        line(c, 345, 125, 280, 280, 18, "#FAFCF9");
        label(c, "PACIFIC HEIGHTS", 12, 25, 9, "#86958C");
        label(c, "NOB HILL", 126, 78, 9, "#86958C");
        label(c, "FINANCIAL", 227, 41, 9, "#86958C");
        label(c, "DISTRICT", 233, 53, 9, "#86958C");
        label(c, "SOMA", 160, 228, 10, "#86958C");
        label(c, "SAN FRANCISCO", 88, 130, 12, "#71867B");
        label(c, "THE BAY", 348, 200, 8, "#7C9F9F");
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (route) {
            routePath.reset();
            routePath.moveTo(143, 185); routePath.lineTo(192, 168); routePath.lineTo(171, 130);
            routePath.lineTo(261, 98); routePath.lineTo(245, 69);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10); color("#FFFFFF"); c.drawPath(routePath, paint);
            paint.setStrokeWidth(5); color("#087F69"); c.drawPath(routePath, paint);
            paint.setStyle(Paint.Style.FILL);
            marker(c, 245, 69, true);
            pill(c, destination.length() > 20 ? destination.substring(0, 18) + "..." : destination,
                186, 27);
        } else {
            car(c, 81, 102, -28);
            car(c, 274, 151, -28);
            car(c, 215, 54, 60);
            car(c, 68, 238, -28);
            pill(c, "3 min away", 157, 125);
        }
        paint.setColor(Color.argb(26, 8, 127, 105));
        c.drawCircle(143, 185, 26, paint);
        marker(c, 143, 185, false);
        if (moving) car(c, 193, 159, -28);
        pill(c, route ? "ILLUSTRATED DEMO ROUTE" : "DEMO MAP  /  SAN FRANCISCO", 12, 256);
        real.restoreToCount(save);
    }

    private void label(Canvas c, String text, float x, float y, int size, String color) {
        color(color); paint.setTextSize(size); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText(text, x, y, paint);
    }

    private void pill(Canvas c, String text, float x, float y) {
        paint.setTextSize(10);
        float w = paint.measureText(text) + 20;
        color("#FFFFFF"); c.drawRoundRect(x, y - 15, x + w, y + 7, 7, 7, paint);
        label(c, text, x + 10, y, 10, "#25443C");
    }

    private void marker(Canvas c, float x, float y, boolean end) {
        color("#FFFFFF"); c.drawCircle(x, y, 10, paint);
        color("#087F69"); c.drawCircle(x, y, 7, paint);
        color("#FFFFFF");
        if (end) c.drawRect(x - 2, y - 2, x + 2, y + 2, paint);
        else c.drawCircle(x, y, 2.5f, paint);
    }

    private void car(Canvas c, float x, float y, float angle) {
        int saved = c.save();
        c.rotate(angle, x, y);
        color("#25443C"); c.drawRoundRect(x - 7, y - 13, x + 7, y + 13, 4, 4, paint);
        color("#B8D3C9"); c.drawRect(x - 5, y - 8, x + 5, y - 3, paint);
        c.drawRect(x - 5, y + 5, x + 5, y + 8, paint);
        c.restoreToCount(saved);
    }
}
