package com.biso.cordova.plugins.mlkit.barcode.scanner;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.getTranslationMatrix;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Utils.mapRect;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private final ScannerSettings settings;

  public BarcodeAnalyzer(ScannerSettings settings, BarcodesListener barcodesListener,
      CameraOverlay cameraOverlay) {
    int useBarcodeFormats = settings.getBarcodeFormats();
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

      if (settings.isDebugOverlay()) {
        cameraOverlay.drawDebugOverlay(detectedBarcodes);
      }

      if (areBarcodesStable(detectedBarcodes) && stableCounter >= settings.getStableThreshold()) {
        ArrayList<DetectedBarcode> barcodesInScanArea = (ArrayList<DetectedBarcode>) detectedBarcodes.stream()
            .filter(barcode -> barcode.isInScanArea(cameraOverlay.getScanArea(), settings.isIgnoreRotatedBarcodes())).sorted()
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
          "barcodes stable for " + stableCounter + "/" + settings.getStableThreshold());
      return true;
    }
    lastBarcodes = barcodes;
    stableCounter = 0;
    return false;
  }
}
