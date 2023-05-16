package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.ScannerSettings.Settings.*;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.getAspectRatioFromString;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import org.json.JSONObject;

public class ScannerSettings implements Parcelable {
  private int barcodeFormats = 1234;
  private String aspectRatio = "1:1";
  private float aspectRatioF = 1;
  private double detectorSize = 0.5;
  private boolean drawFocusRect = true;
  private String focusRectColor = "#FFFFFF";
  private int focusRectBorderRadius = 100;
  private int focusRectBorderThickness = 2;
  private boolean drawFocusLine = true;
  private String focusLineColor = "#FFFFFF";
  private int focusLineThickness = 1;
  private boolean drawFocusBackground = true;
  private String focusBackgroundColor = "#CCFFFFFF";
  private boolean beepOnSuccess = false;
  private boolean vibrateOnSuccess = false;
  private int stableThreshold = 5;
  private boolean debugOverlay = false;
  private boolean ignoreRotatedBarcodes = false;

  public ScannerSettings(JSONObject settings) {
    Iterator<String> keys = settings.keys();

    while(keys.hasNext()) {
      String key = keys.next();

      Optional<Settings> setting = Settings.get(key);
      if(setting.isPresent()) {
        switch (setting.get()) {
          case BARCODE_FORMATS:
            barcodeFormats = settings.optInt(BARCODE_FORMATS.value(), getBarcodeFormats());
            break;
          case DETECTOR_ASPECT_RATIO:
            aspectRatio = settings.optString(DETECTOR_ASPECT_RATIO.value(), getAspectRatio());
            aspectRatioF = getAspectRatioFromString(aspectRatio);
            break;
          case DETECTOR_SIZE:
            double temp = settings.optDouble(DETECTOR_SIZE.value(), getDetectorSize());
            if (!(temp < 0 || temp > 1)) {
              detectorSize = temp;
            }
            break;
          case DRAW_FOCUS_RECT:
            drawFocusRect = settings.optBoolean(DRAW_FOCUS_RECT.value(), isDrawFocusRect());
            break;
          case FOCUS_RECT_COLOR:
            focusRectColor = settings.optString(FOCUS_RECT_COLOR.value(), getFocusRectColor());
            break;
          case FOCUS_RECT_BORDER_RADIUS:
            focusRectBorderRadius = settings.optInt(FOCUS_RECT_BORDER_RADIUS.value(),
                getFocusRectBorderRadius());
            break;
          case FOCUS_RECT_BORDER_THICKNESS:
            focusRectBorderThickness = settings.optInt(FOCUS_RECT_BORDER_THICKNESS.value(),
                getFocusRectBorderThickness());
            break;
          case DRAW_FOCUS_LINE:
            drawFocusLine = settings.optBoolean(DRAW_FOCUS_LINE.value(), isDrawFocusLine());
            break;
          case FOCUS_LINE_COLOR:
            focusLineColor = settings.optString(FOCUS_LINE_COLOR.value(), getFocusLineColor());
            break;
          case FOCUS_LINE_THICKNESS:
            focusLineThickness = settings.optInt(FOCUS_LINE_THICKNESS.value(),
                getFocusLineThickness());
            break;
          case DRAW_FOCUS_BACKGROUND:
            drawFocusBackground = settings.optBoolean(DRAW_FOCUS_BACKGROUND.value(),
                isDrawFocusBackground());
            break;
          case FOCUS_BACKGROUND_COLOR:
            focusBackgroundColor = settings.optString(FOCUS_BACKGROUND_COLOR.value(),
                getFocusBackgroundColor());
            break;
          case BEEP_ON_SUCCESS:
            beepOnSuccess = settings.optBoolean(BEEP_ON_SUCCESS.value(), isBeepOnSuccess());
            break;
          case VIBRATE_ON_SUCCESS:
            vibrateOnSuccess = settings.optBoolean(VIBRATE_ON_SUCCESS.value(),
                isVibrateOnSuccess());
            break;
          case STABLE_THRESHOLD:
            stableThreshold = settings.optInt(STABLE_THRESHOLD.value(), getStableThreshold());
            break;
          case DEBUG_OVERLAY:
            debugOverlay = settings.optBoolean(DEBUG_OVERLAY.value(), isDebugOverlay());
            break;
          case IGNORE_ROTATED_BARCODES:
            ignoreRotatedBarcodes = settings.optBoolean(IGNORE_ROTATED_BARCODES.value(),
                isIgnoreRotatedBarcodes());
            break;
          default:
            Log.e("SETTINGS", "No known setting for " + key);
            break;
        }
      } else {
        Log.e("SETTINGS", "No known setting for " + key);
      }
    }
  }

  public int getBarcodeFormats() {
    return barcodeFormats;
  }

  public String getAspectRatio() {
    return aspectRatio;
  }

  public float getAspectRatioF() {
    return aspectRatioF;
  }

  public double getDetectorSize() {
    return detectorSize;
  }

  public boolean isDrawFocusRect() {
    return drawFocusRect;
  }

  public String getFocusRectColor() {
    return focusRectColor;
  }

  public int getFocusRectBorderRadius() {
    return focusRectBorderRadius;
  }

  public int getFocusRectBorderThickness() {
    return focusRectBorderThickness;
  }

  public boolean isDrawFocusLine() {
    return drawFocusLine;
  }

  public String getFocusLineColor() {
    return focusLineColor;
  }

  public int getFocusLineThickness() {
    return focusLineThickness;
  }

  public boolean isDrawFocusBackground() {
    return drawFocusBackground;
  }

  public String getFocusBackgroundColor() {
    return focusBackgroundColor;
  }

  public boolean isBeepOnSuccess() {
    return beepOnSuccess;
  }

  public boolean isVibrateOnSuccess() {
    return vibrateOnSuccess;
  }

  public int getStableThreshold() {
    return stableThreshold;
  }

  public boolean isDebugOverlay() {
    return debugOverlay;
  }

  public boolean isIgnoreRotatedBarcodes() {
    return ignoreRotatedBarcodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScannerSettings that = (ScannerSettings) o;
    return getBarcodeFormats() == that.getBarcodeFormats()
        && Float.compare(that.getAspectRatioF(), getAspectRatioF()) == 0
        && Double.compare(that.getDetectorSize(), getDetectorSize()) == 0
        && isDrawFocusRect() == that.isDrawFocusRect()
        && getFocusRectBorderRadius() == that.getFocusRectBorderRadius()
        && getFocusRectBorderThickness() == that.getFocusRectBorderThickness()
        && isDrawFocusLine() == that.isDrawFocusLine()
        && getFocusLineThickness() == that.getFocusLineThickness()
        && isDrawFocusBackground() == that.isDrawFocusBackground()
        && isBeepOnSuccess() == that.isBeepOnSuccess()
        && isVibrateOnSuccess() == that.isVibrateOnSuccess()
        && getStableThreshold() == that.getStableThreshold()
        && isDebugOverlay() == that.isDebugOverlay()
        && isIgnoreRotatedBarcodes() == that.isIgnoreRotatedBarcodes()
        && getAspectRatio().equals(that.getAspectRatio()) && getFocusRectColor().equals(
        that.getFocusRectColor())
        && getFocusLineColor().equals(that.getFocusLineColor()) && getFocusBackgroundColor().equals(
        that.getFocusBackgroundColor());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBarcodeFormats(), getAspectRatio(), getAspectRatioF(), getDetectorSize(),
        isDrawFocusRect(),
        getFocusRectColor(), getFocusRectBorderRadius(), getFocusRectBorderThickness(),
        isDrawFocusLine(),
        getFocusLineColor(), getFocusLineThickness(), isDrawFocusBackground(),
        getFocusBackgroundColor(),
        isBeepOnSuccess(),
        isVibrateOnSuccess(), getStableThreshold(), isDebugOverlay(), isIgnoreRotatedBarcodes());
  }

  @Override
  public int describeContents() {
    return hashCode();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.getBarcodeFormats());
    dest.writeString(this.getAspectRatio());
    dest.writeFloat(this.getAspectRatioF());
    dest.writeDouble(this.getDetectorSize());
    dest.writeByte(this.isDrawFocusRect() ? (byte) 1 : (byte) 0);
    dest.writeString(this.getFocusRectColor());
    dest.writeInt(this.getFocusRectBorderRadius());
    dest.writeInt(this.getFocusRectBorderThickness());
    dest.writeByte(this.isDrawFocusLine() ? (byte) 1 : (byte) 0);
    dest.writeString(this.getFocusLineColor());
    dest.writeInt(this.getFocusLineThickness());
    dest.writeByte(this.isDrawFocusBackground() ? (byte) 1 : (byte) 0);
    dest.writeString(this.getFocusBackgroundColor());
    dest.writeByte(this.isBeepOnSuccess() ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isVibrateOnSuccess() ? (byte) 1 : (byte) 0);
    dest.writeInt(this.getStableThreshold());
    dest.writeByte(this.isDebugOverlay() ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isIgnoreRotatedBarcodes() ? (byte) 1 : (byte) 0);
  }

  public void readFromParcel(Parcel source) {
    this.barcodeFormats = source.readInt();
    this.aspectRatio = source.readString();
    this.aspectRatioF = source.readFloat();
    this.detectorSize = source.readDouble();
    this.drawFocusRect = source.readByte() != 0;
    this.focusRectColor = source.readString();
    this.focusRectBorderRadius = source.readInt();
    this.focusRectBorderThickness = source.readInt();
    this.drawFocusLine = source.readByte() != 0;
    this.focusLineColor = source.readString();
    this.focusLineThickness = source.readInt();
    this.drawFocusBackground = source.readByte() != 0;
    this.focusBackgroundColor = source.readString();
    this.beepOnSuccess = source.readByte() != 0;
    this.vibrateOnSuccess = source.readByte() != 0;
    this.stableThreshold = source.readInt();
    this.debugOverlay = source.readByte() != 0;
    this.ignoreRotatedBarcodes = source.readByte() != 0;
  }

  protected ScannerSettings(Parcel in) {
    this.barcodeFormats = in.readInt();
    this.aspectRatio = in.readString();
    this.aspectRatioF = in.readFloat();
    this.detectorSize = in.readDouble();
    this.drawFocusRect = in.readByte() != 0;
    this.focusRectColor = in.readString();
    this.focusRectBorderRadius = in.readInt();
    this.focusRectBorderThickness = in.readInt();
    this.drawFocusLine = in.readByte() != 0;
    this.focusLineColor = in.readString();
    this.focusLineThickness = in.readInt();
    this.drawFocusBackground = in.readByte() != 0;
    this.focusBackgroundColor = in.readString();
    this.beepOnSuccess = in.readByte() != 0;
    this.vibrateOnSuccess = in.readByte() != 0;
    this.stableThreshold = in.readInt();
    this.debugOverlay = in.readByte() != 0;
    this.ignoreRotatedBarcodes = in.readByte() != 0;
  }

  public static final Creator<ScannerSettings> CREATOR = new Creator<ScannerSettings>() {
    @Override
    public ScannerSettings createFromParcel(Parcel source) {
      return new ScannerSettings(source);
    }

    @Override
    public ScannerSettings[] newArray(int size) {
      return new ScannerSettings[size];
    }
  };

  public enum Settings {

    BARCODE_FORMATS("barcodeFormats"),
    DETECTOR_ASPECT_RATIO("detectorAspectRatio"),
    DETECTOR_SIZE("detectorSize"),
    ROTATE_CAMERA("rotateCamera"),
    DRAW_FOCUS_RECT("drawFocusRect"),
    FOCUS_RECT_COLOR("focusRectColor"),
    FOCUS_RECT_BORDER_RADIUS("focusRectBorderRadius"),
    FOCUS_RECT_BORDER_THICKNESS("focusRectBorderThickness"),
    DRAW_FOCUS_LINE("drawFocusLine"),
    FOCUS_LINE_COLOR("focusLineColor"),
    FOCUS_LINE_THICKNESS("focusLineThickness"),
    DRAW_FOCUS_BACKGROUND("drawFocusBackground"),
    FOCUS_BACKGROUND_COLOR("focusBackgroundColor"),
    BEEP_ON_SUCCESS("beepOnSuccess"),
    VIBRATE_ON_SUCCESS("vibrateOnSuccess"),
    STABLE_THRESHOLD("stableThreshold"),
    DEBUG_OVERLAY("debugOverlay"),
    IGNORE_ROTATED_BARCODES("ignoreRotatedBarcodes");

    private final String option;

    Settings(String option) {
      this.option = option;
    }

    public String value() {
      return option;
    }

    public static Optional<Settings> get(String option) {
      return Arrays.stream(Settings.values())
          .filter(o -> o.option.equals(option))
          .findFirst();
    }
  }
}
