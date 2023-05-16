package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.BarcodeAnalyzer.BARCODES;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import com.google.android.gms.common.api.CommonStatusCodes;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MLKitBarcodeScanner extends CordovaPlugin {

  public static final String SETTINGS = "settings";
  private static final int RC_BARCODE_CAPTURE = 9001;
  private CallbackContext callbackContext;
  private ScannerSettings scannerSettings;
  private MediaPlayer mediaPlayer;
  private Vibrator vibrator;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Context context = cordova.getContext();

    VibratorManager vibratorManager = (VibratorManager) context.getSystemService(
        Context.VIBRATOR_MANAGER_SERVICE);
    vibrator = vibratorManager.getDefaultVibrator();
    mediaPlayer = new MediaPlayer();

    try (AssetFileDescriptor descriptor = context.getAssets().openFd("beep.mp3")) {
      mediaPlayer.setAudioAttributes(
          new AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .setUsage(AudioAttributes.USAGE_NOTIFICATION)
              .build()
      );
      mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
          descriptor.getLength());
      mediaPlayer.prepare();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;

    if (action.equals("startScan")) {
      class OneShotTask implements Runnable {

        private final Context context;
        private final JSONArray args;

        private OneShotTask(Context ctx, JSONArray as) {
          context = ctx;
          args = as;
        }

        public void run() {
          try {
            openNewActivity(context, args);
          } catch (JSONException e) {
            MLKitBarcodeScanner.this.callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.ERROR, "JSON_EXCEPTION"));
          }
        }
      }
      Thread t = new Thread(new OneShotTask(cordova.getContext(), args));
      t.start();
      return true;
    }
    return false;
  }

  private void openNewActivity(Context context, JSONArray args) throws JSONException {
    JSONObject config = args.getJSONObject(0);
    Intent intent = new Intent(context, CaptureActivity.class);

    scannerSettings = new ScannerSettings(config);

    intent.putExtra(SETTINGS, scannerSettings);

    this.cordova.setActivityResultCallback(this);
    this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == RC_BARCODE_CAPTURE) {
      if (resultCode == CommonStatusCodes.SUCCESS) {
        if (data != null) {
          try {
            ArrayList<DetectedBarcode> barcodes = data.getParcelableArrayListExtra(BARCODES);
            JSONArray resultBarcodes = new JSONArray();
            for (DetectedBarcode barcode : barcodes) {
              Log.d("MLKitBarcodeScanner", "Barcode read: " + barcode);
              resultBarcodes.put(barcode.getAsJson());
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, resultBarcodes));
          } catch (JSONException e) {
            callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.ERROR, "JSON_EXCEPTION"));
            return;
          }

          if (scannerSettings.isBeepOnSuccess()) {
            mediaPlayer.start();
          }

          if (scannerSettings.isVibrateOnSuccess()) {
            int duration = 200;
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
          }
        }
      } else {
        String err = data.getStringExtra("error");
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, err));
      }
    }
  }

  @Override
  public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;
  }
}
