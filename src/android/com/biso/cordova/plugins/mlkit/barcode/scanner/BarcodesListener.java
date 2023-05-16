package com.biso.cordova.plugins.mlkit.barcode.scanner;

import android.content.Intent;

public interface BarcodesListener {

  void onBarcodesFound(Intent barcodes);
}
