package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

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

import com.mobisys.cordova.plugins.mlkit.barcode.scanner.BarcodeAnalyzer;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

  private int barcodeFormats;
  private double detectorSize = .5;
  private String detectorAspectRatio;
  private boolean drawFocusRect = true;
  private String focusRectColor = "#FFFFFF";
  private int focusRectBorderThickness = 5;
  private int focusRectBorderRadius = 100;
  private boolean drawFocusLine = true;
  private String focusLineColor = "#FFFFFF";
  private int focusLineThickness = 5;
  private boolean drawFocusBackground = true;
  private String focusBackgroundColor = "#CCFFFFFF";
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
  private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};

  private void handleSettings() {
    // read parameters from the intent used to launch the activity.
    barcodeFormats = getIntent().getIntExtra("BarcodeFormats", 1234);
    if (barcodeFormats == 0 || barcodeFormats == 1234) {
      barcodeFormats = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
    }
    detectorSize = getIntent().getDoubleExtra("DetectorSize", detectorSize);
    detectorAspectRatio = getIntent().getStringExtra("DetectorAspectRatio");

    if (detectorSize <= 0 || detectorSize >= 1) {
      // setting boundary detectorSize must be between 0 and 1.
      detectorSize = 0.5;
    }

    drawFocusRect = getIntent().getBooleanExtra("DrawFocusRect", drawFocusRect);
    focusRectColor = getIntent().getStringExtra("FocusRectColor");
    focusRectBorderRadius = getIntent().getIntExtra("FocusRectBorderRadius", focusRectBorderRadius);
    focusRectBorderThickness = getIntent().getIntExtra("FocusRectBorderThickness",
        focusRectBorderThickness);
    drawFocusLine = getIntent().getBooleanExtra("DrawFocusLine", drawFocusLine);
    focusLineColor = getIntent().getStringExtra("FocusLineColor");
    focusLineThickness = getIntent().getIntExtra("FocusLineThickness", focusLineThickness);
    drawFocusBackground = getIntent().getBooleanExtra("DrawFocusBackground", drawFocusBackground);
    focusBackgroundColor = getIntent().getStringExtra("FocusBackgroundColor");
  }

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

    handleSettings();

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

    BarcodeAnalyzer barcodeAnalyzer = new BarcodeAnalyzer(barcodeFormats, this::processBarcodes, 5);

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

      if (drawFocusLine) {
        drawFocusLine(canvas);
      }

      if (drawFocusRect) {
        drawFocusRect(canvas);
      }

      if (drawFocusBackground) {
        drawFocusBackground(canvas);
      }

      surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }
  }

  /**
   * Draws a rectangle around the scan area Border color and thickness are determined by config
   *
   * @param canvas The canvas to draw on
   */
  private void drawFocusRect(Canvas canvas) {
    // border's properties
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.parseColor(focusRectColor));
    paint.setStrokeWidth(focusRectBorderThickness);

    canvas.drawRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()),
        focusRectBorderRadius, focusRectBorderRadius, paint);
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

    float rectWidth = (float) (Math.min(height, width) * detectorSize);
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
   */
  private void drawFocusLine(Canvas canvas) {
    int height = mCameraView.getHeight();
    int width = mCameraView.getWidth();

    float lineWidth =
        Math.min(height, width) - (float) ((1 - detectorSize) * Math.min(height, width));

    // border's properties
    Paint paint = new Paint();
    paint.setColor(Color.parseColor(focusLineColor));
    paint.setStrokeWidth(focusLineThickness);

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
   */
  private void drawFocusBackground(Canvas canvas) {
    Path path = new Path();
    path.addRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()),
        focusRectBorderRadius, focusRectBorderRadius,
        Path.Direction.CCW);
    canvas.clipOutPath(path);
    canvas.drawColor(Color.parseColor(focusBackgroundColor));
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
