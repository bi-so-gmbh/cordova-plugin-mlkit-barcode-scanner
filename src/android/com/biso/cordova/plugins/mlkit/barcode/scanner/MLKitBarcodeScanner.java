package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.BarcodeAnalyzer.BARCODES;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.BEEP_ON_SUCCESS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DEBUG_OVERLAY;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DETECTOR_SIZE;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_BACKGROUND;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_LINE;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DRAW_FOCUS_RECT;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_BACKGROUND_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_LINE_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_LINE_THICKNESS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_BORDER_RADIUS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_BORDER_THICKNESS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.FOCUS_RECT_COLOR;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.ROTATE_CAMERA;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.STABLE_THRESHOLD;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.VIBRATE_ON_SUCCESS;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import com.biso.cordova.plugins.mlkit.barcode.scanner.BarcodeAnalyzer.DetectedBarcode;
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

  private static final int RC_BARCODE_CAPTURE = 9001;
  private CallbackContext callbackContext;
  private boolean beepOnSuccess;
  private boolean vibrateOnSuccess;
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

    try (AssetFileDescriptor descriptor = context.getAssets().openFd("beep.ogg")) {
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
    intent.putExtra(Settings.BARCODE_FORMATS, config.optInt(Settings.BARCODE_FORMATS, 1234));
    intent.putExtra(Settings.DETECTOR_ASPECT_RATIO,
        config.optString(Settings.DETECTOR_ASPECT_RATIO, "1:1"));
    double detectorSize = config.optDouble(DETECTOR_SIZE, 0.5);
    if (detectorSize <= 0 || detectorSize > 1) {
      // setting boundary detectorSize must be between 0 and 1.
      detectorSize = 0.5;
    }
    intent.putExtra(DETECTOR_SIZE, detectorSize);
    intent.putExtra(ROTATE_CAMERA, config.optBoolean(ROTATE_CAMERA, false));
    intent.putExtra(DRAW_FOCUS_RECT, config.optBoolean(DRAW_FOCUS_RECT, true));
    intent.putExtra(FOCUS_RECT_COLOR, config.optString(FOCUS_RECT_COLOR, "#FFFFFF"));
    intent.putExtra(FOCUS_RECT_BORDER_RADIUS, config.optInt(FOCUS_RECT_BORDER_RADIUS, 100));
    intent.putExtra(FOCUS_RECT_BORDER_THICKNESS, config.optInt(FOCUS_RECT_BORDER_THICKNESS, 5));
    intent.putExtra(DRAW_FOCUS_LINE, config.optBoolean(DRAW_FOCUS_LINE, true));
    intent.putExtra(FOCUS_LINE_COLOR, config.optString(FOCUS_LINE_COLOR, "#FFFFFF"));
    intent.putExtra(FOCUS_LINE_THICKNESS, config.optInt(FOCUS_LINE_THICKNESS, 5));
    intent.putExtra(DRAW_FOCUS_BACKGROUND, config.optBoolean(DRAW_FOCUS_BACKGROUND, true));
    intent.putExtra(FOCUS_BACKGROUND_COLOR, config.optString(FOCUS_BACKGROUND_COLOR, "#CCFFFFFF"));
    intent.putExtra(STABLE_THRESHOLD, config.optInt(STABLE_THRESHOLD, 5));
    intent.putExtra(DEBUG_OVERLAY, config.optBoolean(DEBUG_OVERLAY, false));

    beepOnSuccess = config.optBoolean(BEEP_ON_SUCCESS, false);
    vibrateOnSuccess = config.optBoolean(VIBRATE_ON_SUCCESS, false);

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
              JSONArray result = new JSONArray();
              result.put(barcode.getValue());
              result.put(barcode.getFormat());
              result.put(barcode.getType());
              result.put(barcode.getDistanceToCenter());

              Log.d("MLKitBarcodeScanner", "Barcode read: " + barcode);

              resultBarcodes.put(result);
            }
            // for now just get the first barcode we find, they should be sorted by distance to center
            // in the future (once IOS is done) we will return all of them
            callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.OK, resultBarcodes.getJSONArray(0)));
          } catch (JSONException e) {
            callbackContext.sendPluginResult(
                new PluginResult(PluginResult.Status.ERROR, "JSON_EXCEPTION"));
            return;
          }

          if (beepOnSuccess) {
            mediaPlayer.start();
          }

          if (vibrateOnSuccess) {
            int duration = 200;
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
          }
        }
      } else {
        String err = data.getStringExtra("error");
        JSONArray result = new JSONArray();
        result.put(err);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
      }
    }
  }

  @Override
  public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;
  }
}
