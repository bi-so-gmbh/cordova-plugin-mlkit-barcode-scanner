package com.biso.cordova.plugins.mlkit.barcode.scanner;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

public class DetectedBarcode implements Parcelable, Comparable<DetectedBarcode> {

  private static final String DETECTED_BARCODE = "ANALYZER";
  private final RectF bounds;
  private String value;
  private final int format;
  private final int type;
  private final double distanceToCenter;
  private final boolean isPortrait;

  public DetectedBarcode(@NonNull Barcode barcode, @NonNull Pair<RectF, Boolean> boundsAndOrientation, float centerX,
      float centerY) {
    format = barcode.getFormat();
    type = barcode.getValueType();
    value = barcode.getRawValue();
    this.bounds = boundsAndOrientation.first;
    this.isPortrait = boundsAndOrientation.second;

    // rawValue returns null if string is not UTF-8 encoded.
    // If that's the case, we will decode it as ASCII,
    // because it's the most common encoding for barcodes.
    // e.g. https://www.barcodefaq.com/1d/code-128/
    if (value == null) {
      value = new String(barcode.getRawBytes(), StandardCharsets.US_ASCII);
    }

    distanceToCenter = Math.hypot(centerX - this.bounds.centerX(),
        centerY - this.bounds.centerY());
  }

  public DetectedBarcode(Parcel in) {
    value = in.readString();
    format = in.readInt();
    type = in.readInt();
    distanceToCenter = in.readDouble();
    bounds = in.readTypedObject(RectF.CREATOR);
    isPortrait = in.readBoolean();
  }

  public boolean isInScanArea(RectF scanArea) {
    if (isPortrait) {
      return false;
    }

    RectF center = new RectF(bounds.left, bounds.centerY(), bounds.right, bounds.centerY());
    boolean contained = scanArea.contains(center);
    Log.d(DETECTED_BARCODE, center.toShortString() + (contained ? " in " : " not in ") + scanArea.toShortString());
    return contained;
  }

  public RectF getBoundingBox() {
    return bounds;
  }

  public String getValue() {
    return value;
  }

  public int getType() {
    return type;
  }

  public int getFormat() {
    return format;
  }

  public double getDistanceToCenter() {
    return distanceToCenter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DetectedBarcode other = (DetectedBarcode) o;
    return Objects.equals(getValue(), other.getValue()) && Objects.equals(getType(),
        other.getType()) && Objects.equals(getFormat(), other.getFormat());
  }

  @Override
  public int hashCode() {
    int result = 1;
    int prime = 13;
    result += prime * value.hashCode();
    result += prime * type;
    result += prime * format;
    return result;
  }

  @Override
  @NonNull
  public String toString() {
    return "(" + getValue() + ", " + getDistanceToCenter() + ", " + bounds.toShortString() + ")";
  }

  @Override
  public int compareTo(DetectedBarcode o) {
    return Double.compare(distanceToCenter, o.getDistanceToCenter());
  }

  @Override
  public int describeContents() {
    return hashCode();
  }

  @Override
  public void writeToParcel(Parcel out, int i) {
    out.writeString(value);
    out.writeInt(format);
    out.writeInt(type);
    out.writeDouble(distanceToCenter);
    out.writeTypedObject(bounds, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
    out.writeBoolean(isPortrait);
  }

  public JSONObject getAsJson() throws JSONException {
    JSONObject result = new JSONObject();
      result.put("value", getValue());
      result.put("format", getFormat());
      result.put("type", getType());
      result.put("distanceToCenter", getDistanceToCenter());
    return result;
  }

  public static final Parcelable.Creator<DetectedBarcode> CREATOR = new Parcelable.Creator<DetectedBarcode>() {
    public DetectedBarcode createFromParcel(Parcel in) {
      return new DetectedBarcode(in);
    }

    public DetectedBarcode[] newArray(int size) {
      return new DetectedBarcode[size];
    }
  };
}
