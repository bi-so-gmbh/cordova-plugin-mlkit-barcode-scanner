package com.biso.cordova.plugins.mlkit.barcode.scanner;

import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.BARCODE_FORMATS;
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.DETECTOR_ASPECT_RATIO;
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
import static com.biso.cordova.plugins.mlkit.barcode.scanner.Settings.STABLE_THRESHOLD;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Path;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

  public static final String BARCODE_FORMAT = "MLKitBarcodeFormat";
  public static final String BARCODE_TYPE = "MLKitBarcodeType";
  public static final String BARCODE_VALUE = "MLKitBarcodeValue";
  public static final String DISTANCE_TO_CENTER = "distanceToCenter";
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private PreviewView mCameraView;
  private SurfaceView surfaceView;
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  private Camera camera;
  private ScaleGestureDetector scaleGestureDetector;
  private GestureDetector gestureDetector;

  private Bundle settings;
  private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    try {
      if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) || cameraManager.getCameraIdList().length == 0) {
        finishWithError("NO_CAMERA");
      }
    } catch (CameraAccessException e) {
      finishWithError("NO_CAMERA");
    }
    setContentView(getResources().getIdentifier("capture_activity", "layout", getPackageName()));

    settings = getIntent().getExtras();

    // Create the bounding box
    surfaceView = findViewById(getResources().getIdentifier("overlay", "id", getPackageName()));
    surfaceView.setZOrderOnTop(true);

    SurfaceHolder holder = surfaceView.getHolder();
    holder.setFormat(PixelFormat.TRANSPARENT);
    holder.addCallback(this);

    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      startCamera();
    } else {
      requestPermissions(PERMISSIONS, RC_HANDLE_CAMERA_PERM);
    }

    gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener());
    scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

    ImageButton torchButton = findViewById(
        getResources().getIdentifier("torch_button", "id", this.getPackageName()));

    torchButton.setOnClickListener(v -> {
      LiveData<Integer> flashState = camera.getCameraInfo().getTorchState();
      if (flashState.getValue() != null) {
        boolean state = flashState.getValue() == 1;
        torchButton.setBackgroundResource(
            getResources().getIdentifier(!state ? "torch_active" : "torch_inactive",
                "drawable", CaptureActivity.this.getPackageName()));
        camera.getCameraControl().enableTorch(!state);
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == RC_HANDLE_CAMERA_PERM) {
      if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startCamera();
        return;
      }
      finishWithError("NO_CAMERA_PERMISSION");
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    //intentionally empty
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    drawFocus();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    // intentionally empty
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    boolean b = scaleGestureDetector.onTouchEvent(e);
    boolean c = gestureDetector.onTouchEvent(e);

    return b || c || super.onTouchEvent(e);
  }

  private void finishWithError(String errorMessage) {
    Intent result = new Intent();
    result.putExtra("error", errorMessage);
    setResult(CommonStatusCodes.ERROR, result);
    finish();
  }

  void startCamera() {
    mCameraView = findViewById(getResources().getIdentifier("previewView", "id", getPackageName()));
    mCameraView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

    boolean rotateCamera = getIntent().getBooleanExtra("RotateCamera", false);
    if (rotateCamera) {
      mCameraView.setScaleX(-1F);
      mCameraView.setScaleY(-1F);
    } else {
      mCameraView.setScaleX(1F);
      mCameraView.setScaleY(1F);
    }

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(
        this);
    cameraProviderFuture.addListener(() -> {
      try {
        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
        CaptureActivity.this.bindPreview(cameraProvider);

      } catch (ExecutionException | InterruptedException e) {
        // No errors need to be handled for this Future.
        // This should never be reached.
      }
    }, ContextCompat.getMainExecutor(this));
  }

  /**
   * Binding to camera
   */
  private void bindPreview(ProcessCameraProvider cameraProvider) {
    Preview preview = new Preview.Builder().build();

    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK)
        .build();

    preview.setSurfaceProvider(mCameraView.getSurfaceProvider());

    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build();

    BarcodeAnalyzer barcodeAnalyzer = new BarcodeAnalyzer(settings.getInt(BARCODE_FORMATS), this::processBarcodes, settings.getInt(STABLE_THRESHOLD));

    imageAnalysis.setAnalyzer(executor, barcodeAnalyzer);

    camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
  }

  public void processBarcodes(List<Barcode> detectedBarcodes, int width, int height, int rotation) {
    if (!detectedBarcodes.isEmpty()) {
      ArrayList<Bundle> barcodesInScanArea = new ArrayList<>();
      RectF scanArea;

      if (rotation == 90) {
        scanArea = calculateRectF(width, height);
      } else {
        scanArea = calculateRectF(height, width);
      }

      for (Barcode barcode : detectedBarcodes) {
        RectF barcodeBounds = new RectF(barcode.getBoundingBox());
        if (scanArea.contains(barcodeBounds)) {
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
          double distanceToCenter = Math.hypot(scanArea.centerX() - barcodeBounds.centerX(),
              scanArea.centerY() - barcodeBounds.centerY());
          bundle.putDouble(DISTANCE_TO_CENTER, distanceToCenter);

          barcodesInScanArea.add(bundle);
        }
      }

      if (!barcodesInScanArea.isEmpty()) {
        Intent data = new Intent();
        barcodesInScanArea.sort(Comparator.comparingDouble(b -> b.getDouble(DISTANCE_TO_CENTER)));
        data.putParcelableArrayListExtra("barcodes", barcodesInScanArea);
        setResult(CommonStatusCodes.SUCCESS, data);

        finish();
      }
    }
  }


  /**
   * Draws the overlay over the camera preview
   */
  private void drawFocus() {
    if (mCameraView != null) {
      Canvas canvas = surfaceView.getHolder().lockCanvas();
      canvas.drawColor(0, PorterDuff.Mode.CLEAR);

      if (settings.getBoolean(DRAW_FOCUS_LINE)) {
        drawFocusLine(canvas, settings.getString(FOCUS_LINE_COLOR), settings.getInt(FOCUS_LINE_THICKNESS));
      }

      if (settings.getBoolean(DRAW_FOCUS_RECT)) {
        drawScanAreaOutline(canvas, settings.getString(FOCUS_RECT_COLOR), settings.getInt(FOCUS_RECT_BORDER_THICKNESS), settings.getInt(FOCUS_RECT_BORDER_RADIUS));
      }

      if (settings.getBoolean(DRAW_FOCUS_BACKGROUND)) {
        drawFocusBackground(canvas, settings.getString(FOCUS_BACKGROUND_COLOR), settings.getInt(FOCUS_RECT_BORDER_RADIUS));
      }

      surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }
  }

  /**
   * Draws a rectangle outline around the scan area Border color and thickness are determined by config
   *
   * @param canvas The canvas to draw on
   * @param color Hexcolor String of the outline
   * @param thickness thickness of the outline
   * @param radius Corner radius
   */
  private void drawScanAreaOutline(Canvas canvas, String color, int thickness, int radius) {
    // border's properties
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.parseColor(color));
    paint.setStrokeWidth(thickness);

    canvas.drawRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()),
        radius, radius, paint);
  }

  /**
   * Calculates a centered rectangle. Rectangle will: - be centered in the area defined by width x
   * height, - have the aspect ratio set in the config - have a width of min(width, height) *
   * detectorSize set in config
   *
   * @param height height of the area that the rectangle will be centered in
   * @param width  width of the area that the rectangle will be centered in
   * @return rectangle based on float values, centered in the area
   */
  private RectF calculateRectF(int height, int width) {
    float rectAspectRatio = getAspectRatio();

    float rectWidth = (float) (Math.min(height, width) * settings.getDouble(DETECTOR_SIZE));
    float rectHeight = rectWidth / rectAspectRatio;

    float offsetX = width - rectWidth;
    float offsetY = height - rectHeight;

    float left = offsetX / 2;
    float top = offsetY / 2;
    float right = left + rectWidth;
    float bottom = top + rectHeight;

    return new RectF(left, top, right, bottom);
  }

  /**
   * Takes the aspect ratio from the config (which is a String) and turns it into a number.
   *
   * @return The calculated aspect ratio, or 1 in case of error
   */
  private float getAspectRatio() {
    String detectorAspectRatio = settings.getString(DETECTOR_ASPECT_RATIO);
    if (detectorAspectRatio.contains(":")) {
      String[] parts = detectorAspectRatio.split(":");
      if (parts.length == 2) {
        try {
          float left = Integer.parseInt(parts[0]);
          float right = Integer.parseInt(parts[1]);
          return (left / right);
        } catch (NumberFormatException e) {
          // do nothing
        }
      }
    }
    return 1;
  }

  /**
   * Draws a line through the center of the scan area. Color and line thickness are determined by
   * config
   *
   * @param canvas The canvas to draw on
   * @param color Hexcolor String of the line
   * @param thickness thickness of the line
   */
  private void drawFocusLine(Canvas canvas, String color, int thickness) {
    int height = mCameraView.getHeight();
    int width = mCameraView.getWidth();

    float lineWidth =
        Math.min(height, width) - (float) ((1 - settings.getDouble(DETECTOR_SIZE)) * Math.min(height, width));

    // border's properties
    Paint paint = new Paint();
    paint.setColor(Color.parseColor(color));
    paint.setStrokeWidth(thickness);

    float left = width / 2F - lineWidth / 2;
    float top = height / 2F;
    float right = width / 2F + lineWidth / 2;
    float bottom = height / 2F;

    canvas.drawLine(left, top, right, bottom, paint);
  }

  /**
   * Fills out everything but the scan area. Color is determined by config
   *
   * @param canvas The canvas to draw on
   * @param color Hexcolor String of the background (can be with alpha-channel)
   * @param radius Corner radius
   */
  private void drawFocusBackground(Canvas canvas, String color, int radius) {
    Path path = new Path();
    path.addRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()), radius, radius, Path.Direction.CCW);
    canvas.clipOutPath(path);
    canvas.drawColor(Color.parseColor(color));
  }

  // ----------------------------------------------------------------------------
  // | Helper classes
  // ----------------------------------------------------------------------------

  private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      if (camera != null && camera.getCameraInfo().getZoomState().getValue() != null) {
        float scale = camera.getCameraInfo().getZoomState().getValue().getZoomRatio()
            * detector.getScaleFactor();
        camera.getCameraControl().setZoomRatio(scale);
      }
    }
  }
}
