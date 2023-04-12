package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.getTranslationMatrix;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.BaseBundle;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class BarcodeAnalyzer implements Analyzer {

  public static final String BARCODE_FORMAT = "MLKitBarcodeFormat";
  public static final String BARCODE_TYPE = "MLKitBarcodeType";
  public static final String BARCODE_VALUE = "MLKitBarcodeValue";
  public static final String DISTANCE_TO_CENTER = "distanceToCenter";
  private static final String PROCESSOR = "Barcode Analyzer - Processor";
  private static final String STABILIZER = "Barcode Analyzer - Stabilizer";
  private static final String ANALYZER = "Barcode Analyzer - Analyzing";
  private List<Barcode> lastBarcodes;
  private int stableCounter = 0;
  private final int stableThreshold;
  private final BarcodeScanner scanner;
  private final CameraOverlay cameraOverlay;
  private final BarcodesListener barcodesListener;
  private int oldBarcodeSize = -1;

  public BarcodeAnalyzer(int barcodeFormats, int stableThreshold, BarcodesListener barcodesListener, CameraOverlay cameraOverlay) {
    int useBarcodeFormats = barcodeFormats;
    if (useBarcodeFormats == 0 || useBarcodeFormats == 1234) {
      useBarcodeFormats = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
    }
    scanner = BarcodeScanning
        .getClient(
            new BarcodeScannerOptions.Builder().setBarcodeFormats(useBarcodeFormats).build());
    this.lastBarcodes = new ArrayList<>();
    this.stableThreshold = stableThreshold;
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

      if(barcodes.size() != oldBarcodeSize) {
        Log.d(ANALYZER, "Total barcodes detected: " + barcodes.size());
        oldBarcodeSize = barcodes.size();
      }

      if (barcodesStable(barcodes) && stableCounter >= stableThreshold) {
        processBarcodes(barcodes, imageProxy.getCropRect());
      }
    } catch (ExecutionException e) {
      Log.e(ANALYZER, e.getMessage());
    } catch (InterruptedException e) {
      Log.e(ANALYZER, e.getMessage());
      Thread.currentThread().interrupt();
    } finally {
      imageProxy.close();
    }
  }

  private boolean barcodesStable(List<Barcode> barcodes) {
    if (!barcodes.isEmpty() && (barcodes.size() == lastBarcodes.size())) {
        int sameBarcodes = 0;

        for (int i = 0; i < lastBarcodes.size(); i++) {
          String oldBarcodeValue = lastBarcodes.get(i).getRawValue();
          String newBarcodeValue = barcodes.get(i).getRawValue();
          if (oldBarcodeValue == null && newBarcodeValue == null) {
            oldBarcodeValue = new String(lastBarcodes.get(i).getRawBytes(), StandardCharsets.US_ASCII);
            newBarcodeValue = new String(barcodes.get(i).getRawBytes(), StandardCharsets.US_ASCII);
          }

          if (Objects.equals(oldBarcodeValue, newBarcodeValue)) {
            sameBarcodes++;
          }
        }
        if (sameBarcodes == barcodes.size()) {
          Log.d(STABILIZER, "stable for " + stableCounter + "/" + stableThreshold + " cycles");
          stableCounter++;
          return true;
        }
      Log.d(STABILIZER, "unstable, counter reset");
      stableCounter = 0;
    }
    lastBarcodes = barcodes;
    return false;
  }

  public void processBarcodes(List<Barcode> detectedBarcodes, Rect imageBounds) {
    if (!detectedBarcodes.isEmpty()) {
      ArrayList<Bundle> barcodesInScanArea = new ArrayList<>();

      Matrix matrix = getTranslationMatrix(new RectF(imageBounds), cameraOverlay.getSurfaceArea());
      RectF scanArea = cameraOverlay.getScanArea();

      List<RectF> barcodeBoundingBoxes = new ArrayList<>();

      for (Barcode barcode : detectedBarcodes) {
        RectF barcodeBounds = new RectF(barcode.getBoundingBox());
        matrix.mapRect(barcodeBounds);
        barcodeBoundingBoxes.add(barcodeBounds);

        boolean centerLeftInside = scanArea.contains(barcodeBounds.left, barcodeBounds.centerY());
        boolean centerRightInside = scanArea.contains(barcodeBounds.right, barcodeBounds.centerY());
        if (centerLeftInside && centerRightInside) {
          Bundle bundle = new Bundle();
          String value = barcode.getRawValue();

          // rawValue returns null if string is not UTF-8 encoded.
          // If that's the case, we will decode it as ASCII,
          // because it's the most common encoding for barcodes.
          // e.g. https://www.barcodefaq.com/1d/code-128/
          if (value == null) {
            value = new String(barcode.getRawBytes(), StandardCharsets.US_ASCII);
          }

          bundle.putInt(BARCODE_FORMAT, barcode.getFormat());
          bundle.putInt(BARCODE_TYPE, barcode.getValueType());
          bundle.putString(BARCODE_VALUE, value);
          double distanceToCenter = Math.hypot(
              scanArea.centerX() - barcodeBounds.centerX(),
              scanArea.centerY() - barcodeBounds.centerY());
          bundle.putDouble(DISTANCE_TO_CENTER, distanceToCenter);

          barcodesInScanArea.add(bundle);
        }
        Log.d(PROCESSOR, "("+barcodeBounds.left +", " + barcodeBounds.centerY()+")" + (centerLeftInside ? " in " : " not in ") + scanArea.toShortString());
        Log.d(PROCESSOR, "("+barcodeBounds.right +", " + barcodeBounds.centerY()+")" + (centerRightInside ? " in " : " not in ") + scanArea.toShortString());
      }

      cameraOverlay.drawDetectedBarcodesOutlines(barcodeBoundingBoxes);

      if (!barcodesInScanArea.isEmpty()) {
        Intent data = new Intent();
        barcodesInScanArea.sort(Comparator.comparingDouble(b -> b.getDouble(DISTANCE_TO_CENTER)));
        Log.d(PROCESSOR, "found barcodes in scan area: " + Arrays.toString(
            barcodesInScanArea.stream().map(
                BaseBundle::keySet).toArray()));
        data.putParcelableArrayListExtra("barcodes", barcodesInScanArea);
        barcodesListener.onBarcodesFound(data);
      }
    }
  }
}
