package com.biso.cordova.plugins.mlkit.barcode.scanner;

import com.google.mlkit.vision.barcode.common.Barcode;
import java.util.List;

public interface BarcodesListener {

  void onBarcodesFound(List<Barcode> barcodes, int width, int height, int rotation);
}
