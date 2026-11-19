package com.glide.taxi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class SymbolView extends View {
    private final Paint paint = new Paint(3);
    private final Path path = new Path();
    private final Path pin = new Path();
    private final String kind;

    public SymbolView(Context context) {
        this(context, "car", android.graphics.Color.DKGRAY);
    }

    SymbolView(Context context, String kind, int color) {
        super(context);
        this.kind = kind;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int saved = canvas.save();
        canvas.scale(getWidth() / 24f, getHeight() / 24f);
        switch (kind) {
            case "car" -> {
                canvas.drawRoundRect(3, 10, 21, 19, 3, 3, paint);
                path(canvas, 5, 10, 7, 5, 17, 5, 19, 10);
                canvas.drawLine(4, 13, 7, 13, paint);
                canvas.drawLine(17, 13, 20, 13, paint);
                canvas.drawLine(6, 19, 6, 21, paint);
                canvas.drawLine(18, 19, 18, 21, paint);
                canvas.drawLine(10, 16, 14, 16, paint);
            }
            case "home" -> {
                path(canvas, 3, 11, 12, 3, 21, 11);
                path(canvas, 5, 10, 5, 21, 10, 21, 10, 15, 14, 15, 14, 21, 19, 21, 19, 10);
            }
            case "work" -> {
                canvas.drawRoundRect(3, 7, 21, 21, 2, 2, paint);
                path(canvas, 8, 7, 8, 3, 16, 3, 16, 7);
                canvas.drawLine(3, 12, 21, 12, paint);
                canvas.drawLine(10, 12, 14, 12, paint);
            }
            case "clock" -> {
                canvas.drawCircle(12, 12, 9, paint);
                path(canvas, 12, 6, 12, 12, 16, 14);
            }
            case "person" -> {
                canvas.drawCircle(12, 7, 4, paint);
                canvas.drawArc(4, 13, 20, 27, 180, 180, false, paint);
            }
            default -> {
                pin.reset();
                pin.moveTo(12, 22);
                pin.cubicTo(9, 18, 4, 13, 4, 9);
                pin.cubicTo(4, -1, 20, -1, 20, 9);
                pin.cubicTo(20, 13, 15, 18, 12, 22);
                canvas.drawPath(pin, paint);
                canvas.drawCircle(12, 9, 2.5f, paint);
            }
        }
        canvas.restoreToCount(saved);
    }

    private void path(Canvas canvas, float... points) {
        path.reset();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        canvas.drawPath(path, paint);
    }
}
