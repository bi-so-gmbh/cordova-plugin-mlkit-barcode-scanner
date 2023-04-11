package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DETECTOR_ASPECT_RATIO;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DETECTOR_SIZE;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_BACKGROUND;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_LINE;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_RECT;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_BACKGROUND_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_LINE_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_LINE_THICKNESS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_BORDER_RADIUS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_BORDER_THICKNESS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.calculateRectF;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.getAspectRatioFromString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class CameraOverlay extends SurfaceView implements Callback {

  private final Bundle settings;
  private final float aspectRatio;
  private RectF scanArea;
  private RectF surfaceArea;

  public CameraOverlay(Context context, Bundle settings) {
    super(context);
    this.settings = settings;
    aspectRatio = getAspectRatioFromString(settings.getString(DETECTOR_ASPECT_RATIO));
    setZOrderOnTop(true);
    SurfaceHolder holder = getHolder();
    holder.setFormat(PixelFormat.TRANSPARENT);
    holder.addCallback(this);
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    // intentionally empty
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    surfaceArea = new RectF(surfaceHolder.getSurfaceFrame());
    scanArea = calculateRectF(surfaceHolder.getSurfaceFrame().height(),
        surfaceHolder.getSurfaceFrame().width(),
        settings.getDouble(DETECTOR_SIZE), aspectRatio);

    Canvas canvas = surfaceHolder.lockCanvas();
    canvas.drawColor(0, PorterDuff.Mode.CLEAR);

    if (settings.getBoolean(DRAW_FOCUS_LINE)) {
      drawFocusLine(canvas, settings.getString(FOCUS_LINE_COLOR),
          settings.getInt(FOCUS_LINE_THICKNESS));
    }

    if (settings.getBoolean(DRAW_FOCUS_RECT)) {
      drawScanAreaOutline(canvas, settings.getString(FOCUS_RECT_COLOR),
          settings.getInt(FOCUS_RECT_BORDER_THICKNESS),
          settings.getInt(FOCUS_RECT_BORDER_RADIUS));
    }

    if (settings.getBoolean(DRAW_FOCUS_BACKGROUND)) {
      drawFocusBackground(canvas, settings.getString(FOCUS_BACKGROUND_COLOR),
          settings.getInt(FOCUS_RECT_BORDER_RADIUS));
    }

    surfaceHolder.unlockCanvasAndPost(canvas);
  }

  public RectF getScanArea() {
    return scanArea;
  }

  public RectF getSurfaceArea() {
    return surfaceArea;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    // intentionally empty
  }

  /**
   * Draws a rectangle outline around the scan area
   *
   * @param canvas    The canvas to draw on
   * @param color     String with the color in hexadecimal format
   * @param thickness thickness of the outline
   * @param radius    Corner radius
   */
  private void drawScanAreaOutline(Canvas canvas, String color, int thickness, int radius) {
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.parseColor(color));
    paint.setStrokeWidth(thickness);

    canvas.drawRoundRect(scanArea, radius, radius, paint);
  }

  /**
   * Draws a line through the center of the scan area
   *
   * @param canvas    The canvas to draw on
   * @param color     String with the color in hexadecimal format
   * @param thickness thickness of the line
   */
  private void drawFocusLine(Canvas canvas, String color, int thickness) {
    Paint paint = new Paint();
    paint.setColor(Color.parseColor(color));
    paint.setStrokeWidth(thickness);

    canvas.drawLine(scanArea.left, scanArea.centerY(), scanArea.right, scanArea.centerY(), paint);
  }

  /**
   * Fills out everything but the scan area
   *
   * @param canvas The canvas to draw on
   * @param color  String with the color in hexadecimal format (can contain alpha-channel)
   * @param radius Corner radius
   */
  private void drawFocusBackground(Canvas canvas, String color, int radius) {
    Path path = new Path();
    path.addRoundRect(scanArea, radius,
        radius, Path.Direction.CCW);
    canvas.clipOutPath(path);
    canvas.drawColor(Color.parseColor(color));
  }
}
