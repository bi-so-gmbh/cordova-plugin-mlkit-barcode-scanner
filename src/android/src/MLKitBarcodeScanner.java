package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;

import java.util.ArrayList;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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

    VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
    vibrator = vibratorManager.getDefaultVibrator();
    mediaPlayer = new MediaPlayer();

    try {
      AssetFileDescriptor descriptor = context.getAssets().openFd("beep.ogg");
      mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
      descriptor.close();
      mediaPlayer.prepare();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    Activity activity = cordova.getActivity();
    boolean hasCamera = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

    this.callbackContext = callbackContext;

    int numberOfCameras = 0;

    try {
      numberOfCameras = cameraManager.getCameraIdList().length;
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (!hasCamera || numberOfCameras == 0) {
      AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(activity.getString(activity.getResources()
          .getIdentifier("no_cameras_found", "string", activity.getPackageName())));
      alertDialog.setButton(
          DialogInterface.BUTTON_POSITIVE, activity.getString(activity.getResources()
              .getIdentifier("ok", "string", activity.getPackageName())),
          (dialog, which) -> dialog.dismiss());
      alertDialog.show();
      return false;
    }

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
            MLKitBarcodeScanner.this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
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
    intent.putExtra("BarcodeFormats", config.optInt("barcodeFormats", 1234));
    intent.putExtra("DetectorWidth", config.optDouble("detectorWidth", 0.5));
    intent.putExtra("DetectorHeight", config.optDouble("detectorHeight", 0.5));
    intent.putExtra("RotateCamera", config.optBoolean("rotateCamera", false));
    intent.putExtra("DrawFocusRect", config.optBoolean("drawFocusRect", true));
    intent.putExtra("FocusRectColor", config.optString("focusRectColor", "#FFFFFF"));
    intent.putExtra("FocusRectBorderRadius", config.optInt("focusRectBorderRadius", 100));
    intent.putExtra("FocusRectBorderThickness", config.optInt("focusRectBorderThickness", 5));
    intent.putExtra("ScanAreaAdjustment", config.optInt("scanAreaAdjustment", 0));
    intent.putExtra("DrawFocusLine", config.optBoolean("drawFocusLine", true));
    intent.putExtra("FocusLineColor", config.optString("focusLineColor", "#FFFFFF"));
    intent.putExtra("FocusLineThickness", config.optInt("focusLineThickness", 5));
    intent.putExtra("DrawFocusBackground", config.optBoolean("drawFocusBackground", true));
    intent.putExtra("FocusBackgroundColor", config.optString("focusBackgroundColor", "#CCFFFFFF"));

    beepOnSuccess = config.optBoolean("beepOnSuccess", false);
    vibrateOnSuccess = config.optBoolean("vibrateOnSuccess", false);

    this.cordova.setActivityResultCallback(this);
    this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == RC_BARCODE_CAPTURE) {
      if (resultCode == CommonStatusCodes.SUCCESS) {
        if (data != null) {
          ArrayList<Bundle> barcodes = data.getParcelableArrayListExtra("barcodes");
          JSONArray resultBarcodes = new JSONArray();
          for (Bundle barcode : barcodes) {
            Integer barcodeFormat = barcode.getInt(CaptureActivity.BARCODE_FORMAT, 0);
            Integer barcodeType = barcode.getInt(CaptureActivity.BARCODE_TYPE, 0);
            String barcodeValue = barcode.getString(CaptureActivity.BARCODE_VALUE);

            JSONArray result = new JSONArray();
            result.put(barcodeValue);
            result.put(barcodeFormat);
            result.put(barcodeType);

            Log.d("MLKitBarcodeScanner", "Barcode read: " + barcodeValue);

            resultBarcodes.put(result);
          }
          JSONArray result;
          try {
            result = resultBarcodes.getJSONArray(0);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

          if (beepOnSuccess) {
            mediaPlayer.start();
          }

          if (vibrateOnSuccess) {
            int duration = 200;
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
          }


        }
      } else {
        String err = data.getStringExtra("err");
        JSONArray result = new JSONArray();
        result.put(err);
        result.put("");
        result.put("");
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
      }
    }
  }

  @Override
  public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;
  }
}
