package com.biso.cordova.plugins.mlkit.barcode.scanner;

public final class Settings {

  public static final String BARCODE_FORMATS = "barcodeFormats";
  public static final String DETECTOR_ASPECT_RATIO = "detectorAspectRatio";
  public static final String DETECTOR_SIZE = "detectorSize";
  public static final String ROTATE_CAMERA = "rotateCamera";
  public static final String DRAW_FOCUS_RECT = "drawFocusRect";
  public static final String FOCUS_RECT_COLOR = "focusRectColor";
  public static final String FOCUS_RECT_BORDER_RADIUS = "focusRectBorderRadius";
  public static final String FOCUS_RECT_BORDER_THICKNESS = "focusRectBorderThickness";
  public static final String DRAW_FOCUS_LINE = "drawFocusLine";
  public static final String FOCUS_LINE_COLOR = "focusLineColor";
  public static final String FOCUS_LINE_THICKNESS = "focusLineThickness";
  public static final String DRAW_FOCUS_BACKGROUND = "drawFocusBackground";
  public static final String FOCUS_BACKGROUND_COLOR = "focusBackgroundColor";
  public static final String BEEP_ON_SUCCESS = "beepOnSuccess";
  public static final String VIBRATE_ON_SUCCESS = "vibrateOnSuccess";
  public static final String STABLE_THRESHOLD = "stableThreshold";
  public static final String DEBUG_OVERLAY = "debugOverlay";

  private Settings() {
    throw new IllegalStateException("Utility class");
  }
}
