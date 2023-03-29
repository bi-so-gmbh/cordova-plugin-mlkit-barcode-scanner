package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path;
import android.os.Bundle;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

  private int barcodeFormats;
  private double detectorHeight = .5;
  private double detectorWidth = .5;
  private boolean drawFocusRect = true;
  private String focusRectColor = "#FFFFFF";
  private int focusRectBorderThickness = 5;
  private int focusRectBorderRadius = 100;
  private int scanAreaAdjustment = 0;
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
  private SurfaceHolder holder;
  private SurfaceView surfaceView;
  private Canvas canvas;
  private Paint paint;
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  private Camera camera;
  private ScaleGestureDetector scaleGestureDetector;
  private GestureDetector gestureDetector;
  private static final String STRING = "string";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getResources().getIdentifier("capture_activity", "layout", getPackageName()));

    // Create the bounding box
    surfaceView = findViewById(getResources().getIdentifier("overlay", "id", getPackageName()));
    surfaceView.setZOrderOnTop(true);

    holder = surfaceView.getHolder();
    holder.setFormat(PixelFormat.TRANSPARENT);
    holder.addCallback(this);

    // read parameters from the intent used to launch the activity.
    barcodeFormats = getIntent().getIntExtra("BarcodeFormats", 1234);
    detectorWidth = getIntent().getDoubleExtra("DetectorWidth", detectorWidth);
    detectorHeight = getIntent().getDoubleExtra("DetectorHeight", detectorHeight);

    if (detectorWidth <= 0
        || detectorWidth >= 1) { // setting boundary detectorSize must be between 0 to 1.
      detectorWidth = 0.5;
    }

    if (detectorHeight <= 0
        || detectorHeight >= 1) { // setting boundary detectorSize must be between 0 to 1.
      detectorHeight = 0.5;
    }

    drawFocusRect = getIntent().getBooleanExtra("DrawFocusRect", drawFocusRect);
    focusRectColor = getIntent().getStringExtra("FocusRectColor");
    focusRectBorderRadius = getIntent().getIntExtra("FocusRectBorderRadius", focusRectBorderRadius);
    focusRectBorderThickness = getIntent().getIntExtra("FocusRectBorderThickness",
        focusRectBorderThickness);
    scanAreaAdjustment = getIntent().getIntExtra("ScanAreaAdjustment", scanAreaAdjustment);
    drawFocusLine = getIntent().getBooleanExtra("DrawFocusLine", drawFocusLine);
    focusLineColor = getIntent().getStringExtra("FocusLineColor");
    focusLineThickness = getIntent().getIntExtra("FocusLineThickness", focusLineThickness);
    drawFocusBackground = getIntent().getBooleanExtra("DrawFocusBackground", drawFocusBackground);
    focusBackgroundColor = getIntent().getStringExtra("FocusBackgroundColor");

    int rc = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

    if (rc == PackageManager.PERMISSION_GRANTED) {
      // Start Camera
      startCamera();
    } else {
      requestCameraPermission();
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

      if (camera != null) {
        float scale = camera.getCameraInfo().getZoomState().getValue().getZoomRatio()
            * detector.getScaleFactor();
        camera.getCameraControl().setZoomRatio(scale);
      }
    }
  }

  private void requestCameraPermission() {

    final String[] permissions = new String[]{Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE};

    boolean shouldShowPermission = !ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.CAMERA);
    shouldShowPermission = shouldShowPermission
        && !ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    if (shouldShowPermission) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    View.OnClickListener listener = view -> ActivityCompat.requestPermissions(CaptureActivity.this,
        permissions, RC_HANDLE_CAMERA_PERM);

    findViewById(
        getResources().getIdentifier("topLayout", "id", getPackageName())).setOnClickListener(
        listener);
    Snackbar
        .make(surfaceView,
            getResources().getIdentifier("bcode_permission_camera_rationale", STRING,
                getPackageName()),
            BaseTransientBottomBar.LENGTH_INDEFINITE)
        .setAction(getResources().getIdentifier("bcode_ok", STRING, getPackageName()), listener)
        .show();

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      startCamera();
      drawFocus();
      return;
    }

    DialogInterface.OnClickListener listener = (dialog, id) -> finish();

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Camera permission required")
        .setMessage(
            getResources().getIdentifier("bcode_no_camera_permission", STRING, getPackageName()))
        .setPositiveButton(getResources().getIdentifier("bcode_ok", STRING, getPackageName()),
            listener)
        .show();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    // intentionally empty
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

  @Override
  protected void onPause() {
    super.onPause();

  }

  @Override
  protected void onResume() {
    super.onResume();

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

    int barcodeFormat;
    if (barcodeFormats == 0 || barcodeFormats == 1234) {
      barcodeFormat = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
    } else {
      barcodeFormat = barcodeFormats;
    }

    Preview preview = new Preview.Builder().build();

    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK)
        .build();

    preview.setSurfaceProvider(mCameraView.getSurfaceProvider());

    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .setOutputImageFormat(ImageFormat.RGB_565)
        .build();

    BarcodeScanner scanner = BarcodeScanning
        .getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build());

    imageAnalysis.setAnalyzer(executor, image -> {

      if (image.getImage() == null) {
        return;
      }

      InputImage inputImage = InputImage.fromMediaImage(image.getImage(),
          image.getImageInfo().getRotationDegrees());

      scanner.process(inputImage)
          .addOnSuccessListener(barCodes -> {
            if (!barCodes.isEmpty()) {
              ArrayList<Bundle> barcodes = new ArrayList<>();
              RectF scanArea;
              if (inputImage.getRotationDegrees() % 90 == 0) {
                scanArea = calculateRectF(inputImage.getWidth(), inputImage.getHeight());
              } else {
                scanArea = calculateRectF(inputImage.getHeight(), inputImage.getWidth());
              }
              scanArea = resizeRectF(scanArea, 0, scanAreaAdjustment);

              for (Barcode barcode : barCodes) {
                RectF barcodeBounds = new RectF(barcode.getBoundingBox());
                if (scanArea.contains(barcodeBounds.centerX(), barcodeBounds.centerY())) {
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

                  barcodes.add(bundle);
                }
              }

              if (!barcodes.isEmpty()) {
                Intent data = new Intent();
                barcodes.sort(Comparator.comparingDouble(b -> b.getDouble(DISTANCE_TO_CENTER)));
                data.putParcelableArrayListExtra("barcodes", barcodes);
                setResult(CommonStatusCodes.SUCCESS, data);

                finish();
              }
            }
          }).addOnFailureListener(e -> {
          }).addOnCompleteListener(task -> image.close());
    });

    camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
  }

  private void drawFocus() {
    if (mCameraView != null) {
      canvas = holder.lockCanvas();
      canvas.drawColor(0, PorterDuff.Mode.CLEAR);

      if (drawFocusLine) {
        drawFocusLine();
      }

      if (drawFocusRect) {
        drawFocusRect();
      }

      if (drawFocusBackground) {
        drawFocusBackground();
      }

      holder.unlockCanvasAndPost(canvas);
    }
  }

  /**
   * For drawing the rectangular box
   */
  private void drawFocusRect() {
    // border's properties
    paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.parseColor(focusRectColor));
    paint.setStrokeWidth(focusRectBorderThickness);

    canvas.drawRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()),
        focusRectBorderRadius, focusRectBorderRadius, paint);
  }

  private RectF calculateRectF(int height, int width) {
    int left, right, top, bottom, diameterWidth, diameterHeight;

    diameterWidth = width - (int) ((1 - detectorWidth) * width);
    diameterHeight = height - (int) ((1 - detectorHeight) * height);

    left = width / 2 - diameterWidth / 2;
    top = height / 2 - diameterHeight / 2;
    right = width / 2 + diameterWidth / 2;
    bottom = height / 2 + diameterHeight / 2;

    return new RectF(left, top, right, bottom);
  }

  private Rect calculateRect(int height, int width) {
    Rect rect = new Rect();
    calculateRectF(height, width).roundOut(rect);
    return rect;
  }

  private Rect resizeRect(Rect input, int sizeAdjustmentX, int sizeAdjustmentY) {
    Rect resized = new Rect();
    resized.set(input.left - sizeAdjustmentX, input.top - sizeAdjustmentY,
        input.right + sizeAdjustmentX, input.bottom + sizeAdjustmentY);
    return resized;
  }

  private RectF resizeRectF(RectF input, int sizeAdjustmentX, float sizeAdjustmentY) {
    RectF resized = new RectF();
    resized.set(input.left - sizeAdjustmentX, input.top - sizeAdjustmentY,
        input.right + sizeAdjustmentX, input.bottom + sizeAdjustmentY);
    return resized;
  }

  private void drawFocusLine() {
    int height = mCameraView.getHeight();
    int width = mCameraView.getWidth();

    int left, right, top, bottom, diameterWidth;

    diameterWidth = width - (int) ((1 - detectorWidth) * width);

    // border's properties
    paint = new Paint();
    paint.setColor(Color.parseColor(focusLineColor));
    paint.setStrokeWidth(focusLineThickness);

    left = width / 2 - diameterWidth / 2;
    top = height / 2;
    right = width / 2 + diameterWidth / 2;
    bottom = height / 2;

    canvas.drawLine(left, top, right, bottom, paint);
  }

  private void drawFocusBackground() {
    Path path = new Path();
    path.addRoundRect(calculateRectF(mCameraView.getHeight(), mCameraView.getWidth()),
        focusRectBorderRadius, focusRectBorderRadius,
        Path.Direction.CCW);
    canvas.clipOutPath(path);
    canvas.drawColor(Color.parseColor(focusBackgroundColor));
  }
}
