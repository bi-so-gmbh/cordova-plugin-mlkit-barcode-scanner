package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.BARCODE_FORMATS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DEBUG_OVERLAY;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.STABLE_THRESHOLD;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.getTranslationMatrix;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.mapRect;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BarcodeAnalyzer implements Analyzer {

  public static final String BARCODES = "barcodes";
  private static final String ANALYZER = "BarcodeAnalyzer";
  private List<DetectedBarcode> lastBarcodes;
  private int stableCounter = 0;
  private final BarcodeScanner scanner;
  private final CameraOverlay cameraOverlay;
  private final BarcodesListener barcodesListener;
  private final Bundle settings;

  public BarcodeAnalyzer(Bundle settings, BarcodesListener barcodesListener,
      CameraOverlay cameraOverlay) {
    int useBarcodeFormats = settings.getInt(BARCODE_FORMATS);
    if (useBarcodeFormats == 0 || useBarcodeFormats == 1234) {
      useBarcodeFormats = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
    }
    scanner = BarcodeScanning
        .getClient(
            new BarcodeScannerOptions.Builder().setBarcodeFormats(useBarcodeFormats).build());
    this.settings = settings;
    this.lastBarcodes = new ArrayList<>();
    this.barcodesListener = barcodesListener;
    this.cameraOverlay = cameraOverlay;
  }

  @Override
  public void analyze(@NonNull ImageProxy imageProxy) {
    if (imageProxy.getImage() == null) {
      imageProxy.close();
      return;
    }

    InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(),
        imageProxy.getImageInfo().getRotationDegrees());

    Task<List<Barcode>> scannerTask = scanner.process(inputImage);
    try {
      List<Barcode> barcodes = Tasks.await(scannerTask);

      RectF source;
      if (inputImage.getRotationDegrees() == 90) {
        source = new RectF(0, 0, inputImage.getHeight(), inputImage.getWidth());
      } else {
        source = new RectF(0, 0, inputImage.getWidth(), inputImage.getHeight());
      }
      Matrix matrix = getTranslationMatrix(source, cameraOverlay.getSurfaceArea());

      List<DetectedBarcode> detectedBarcodes = barcodes.stream().map(
              barcode -> new DetectedBarcode(barcode, mapRect(barcode.getBoundingBox(), matrix),
                  cameraOverlay.getScanArea().centerX(), cameraOverlay.getScanArea().centerY()))
          .collect(Collectors.toList());

      if (settings.getBoolean(DEBUG_OVERLAY)) {
        cameraOverlay.drawDebugOverlay(detectedBarcodes);
      }

      if (areBarcodesStable(detectedBarcodes) && stableCounter >= settings.getInt(
          STABLE_THRESHOLD)) {
        ArrayList<DetectedBarcode> barcodesInScanArea = (ArrayList<DetectedBarcode>) detectedBarcodes.stream()
            .filter(barcode -> barcode.isInScanArea(cameraOverlay.getScanArea())).sorted()
            .collect(Collectors.toList());

        if (!barcodesInScanArea.isEmpty()) {
          Intent data = new Intent();
          data.putParcelableArrayListExtra(BARCODES, barcodesInScanArea);
          barcodesListener.onBarcodesFound(data);
        } else {
          stableCounter = 0;
          lastBarcodes = new ArrayList<>();
        }
      }
    } catch (ExecutionException e) {
      Log.e(ANALYZER, e.getMessage());
    } catch (InterruptedException e) {
      Log.e(ANALYZER, e.getMessage());
      Thread.currentThread().interrupt();
    }
    imageProxy.close();
  }

  private boolean areBarcodesStable(List<DetectedBarcode> barcodes) {
    if (!barcodes.isEmpty() && (barcodes.size() == lastBarcodes.size())
        && (new HashSet<>(lastBarcodes).containsAll(barcodes))) {
      stableCounter++;
      Log.d(ANALYZER,
          "barcodes stable for " + stableCounter + "/" + settings.getInt(STABLE_THRESHOLD));
      return true;
    }
    lastBarcodes = barcodes;
    stableCounter = 0;
    return false;
  }

  public static class DetectedBarcode implements Parcelable, Comparable<DetectedBarcode> {

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
      Log.d(ANALYZER,
          center.toShortString() + (contained ? " in " : " not in ") + scanArea.toShortString());
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

    public static final Parcelable.Creator<DetectedBarcode> CREATOR = new Parcelable.Creator<DetectedBarcode>() {
      public DetectedBarcode createFromParcel(Parcel in) {
        return new DetectedBarcode(in);
      }

      public DetectedBarcode[] newArray(int size) {
        return new DetectedBarcode[size];
      }
    };
  }
}
