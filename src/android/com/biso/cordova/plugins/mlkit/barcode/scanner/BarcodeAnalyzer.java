package com.biso.cordova.plugins.mlkit.barcode.scanner;

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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class BarcodeAnalyzer implements Analyzer {
  List<Barcode> lastBarcodes;
  int stableCounter = 0;
  final int stableThreshold;
  final BarcodeScanner scanner;
  final BarcodesListener barcodesListener;

  public BarcodeAnalyzer(int barcodeFormats, BarcodesListener barcodesListener, int stableThreshold) {
    int useBarcodeFormats = barcodeFormats;
    if (useBarcodeFormats == 0 || useBarcodeFormats == 1234) {
      useBarcodeFormats = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
    }
    scanner = BarcodeScanning
        .getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(useBarcodeFormats).build());
    this.barcodesListener = barcodesListener;
    this.lastBarcodes = new ArrayList<>();
    this.stableThreshold = stableThreshold;
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

      if(barcodesStable(barcodes) && stableCounter >= stableThreshold) {
        barcodesListener.onBarcodesFound(barcodes, inputImage.getWidth(), inputImage.getHeight(),
            inputImage.getRotationDegrees());
      }
    } catch (ExecutionException | InterruptedException e) {
      Log.e("analyzing", e.getMessage());
      Thread.currentThread().interrupt();
    } finally {
      imageProxy.close();
    }
  }

  private boolean barcodesStable(List<Barcode> barcodes) {
    if (!barcodes.isEmpty()) {
      if (barcodes.size() == lastBarcodes.size()) {
        int sameBarcodes = 0;

        for (int i = 0; i < lastBarcodes.size(); i++) {
          Log.d("stabilizer",
              "oldBarcode: " + lastBarcodes.get(i).getRawValue() + ", new barcode: " + barcodes.get(
                  i).getRawValue());
          if (Objects.equals(lastBarcodes.get(i).getRawValue(), barcodes.get(i).getRawValue())) {
            Log.d("stabilizer", "barcode equal");
            sameBarcodes++;
          }
        }
        if (sameBarcodes == barcodes.size()) {
          Log.d("stabilizer", "stable for " + stableCounter + " cycles");
          stableCounter++;
          return true;
        }
      }
    }
    lastBarcodes = barcodes;
    stableCounter = 0;
    return false;
  }
}
