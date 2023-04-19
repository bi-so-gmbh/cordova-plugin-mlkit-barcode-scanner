//
//  Copyright (c) 2018 Google Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import AVFoundation
import CoreVideo
import MLImage
import MLKit

@objc(CameraViewController)
class CameraViewController: UIViewController {
  private var isUsingFrontCamera = true
  private var previewLayer: AVCaptureVideoPreviewLayer!
  private lazy var captureSession = AVCaptureSession()
  private lazy var sessionQueue = DispatchQueue(label: Constant.sessionQueueLabel)
  private var lastFrame: CMSampleBuffer?

  private lazy var previewOverlayView: UIImageView = {

    precondition(isViewLoaded)
    let previewOverlayView = UIImageView(frame: .zero)
    previewOverlayView.contentMode = UIView.ContentMode.scaleAspectFill
    previewOverlayView.translatesAutoresizingMaskIntoConstraints = false
    return previewOverlayView
  }()

  private lazy var annotationOverlayView: UIView = {
    precondition(isViewLoaded)
    let annotationOverlayView = UIView(frame: .zero)
    annotationOverlayView.translatesAutoresizingMaskIntoConstraints = false
    return annotationOverlayView
  }()

  // MARK: - UIViewController

  override func viewDidLoad() {
    super.viewDidLoad()

    previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
    setUpPreviewOverlayView()
    setUpAnnotationOverlayView()
    setUpCaptureSessionOutput()
    setUpCaptureSessionInput()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)

    startSession()
  }

  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    stopSession()
  }

  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()

    previewLayer.frame = self.view.frame
  }

  // MARK: - IBActions

  @IBAction func switchCamera(_ sender: Any) {
    isUsingFrontCamera = !isUsingFrontCamera
    removeDetectionAnnotations()
    setUpCaptureSessionInput()
  }

  // MARK: On-Device Detections

  private func scanBarcodesOnDevice(in image: VisionImage, width: CGFloat, height: CGFloat) {
    // Define the options for a barcode detector.
    let format = BarcodeFormat.all
    let barcodeOptions = BarcodeScannerOptions(formats: format)

    // Create a barcode scanner.
    let barcodeScanner = BarcodeScanner.barcodeScanner(options: barcodeOptions)
    var barcodes: [Barcode] = []
    var scanningError: Error?
    do {
      barcodes = try barcodeScanner.results(in: image)
    } catch let error {
      scanningError = error
    }
    weak var weakSelf = self
    DispatchQueue.main.sync {
      guard let strongSelf = weakSelf else {
        print("Self is nil!")
        return
      }
      strongSelf.updatePreviewOverlayViewWithLastFrame()

      if let scanningError = scanningError {
        print("Failed to scan barcodes with error: \(scanningError.localizedDescription).")
        return
      }
      guard !barcodes.isEmpty else {
        print("Barcode scanner returrned no results.")
        return
      }
      for barcode in barcodes {
        let normalizedRect = CGRect(
          x: barcode.frame.origin.x / width,
          y: barcode.frame.origin.y / height,
          width: barcode.frame.size.width / width,
          height: barcode.frame.size.height / height
        )
        let convertedRect = strongSelf.previewLayer.layerRectConverted(
          fromMetadataOutputRect: normalizedRect
        )
        UIUtilities.addRectangle(
          convertedRect,
          to: strongSelf.annotationOverlayView,
          color: UIColor.green
        )
        let label = UILabel(frame: convertedRect)
        label.text = barcode.displayValue
        label.adjustsFontSizeToFitWidth = true
        strongSelf.rotate(label, orientation: image.orientation)
        strongSelf.annotationOverlayView.addSubview(label)
      }
    }
  }

    // MARK: - Private
      private func setUpCaptureSessionOutput() {
        weak var weakSelf = self
        sessionQueue.async {
          guard let strongSelf = weakSelf else {
            print("Self is nil!")
            return
          }
          strongSelf.captureSession.beginConfiguration()
          // When performing latency tests to determine ideal capture settings,
          // run the app in 'release' mode to get accurate performance metrics
          strongSelf.captureSession.sessionPreset = AVCaptureSession.Preset.medium

          let output = AVCaptureVideoDataOutput()
          output.videoSettings = [
            (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA
          ]
          output.alwaysDiscardsLateVideoFrames = true
          let outputQueue = DispatchQueue(label: Constant.videoDataOutputQueueLabel)
          output.setSampleBufferDelegate(strongSelf, queue: outputQueue)
          guard strongSelf.captureSession.canAddOutput(output) else {
            print("Failed to add capture session output.")
            return
          }
          strongSelf.captureSession.addOutput(output)
          strongSelf.captureSession.commitConfiguration()
        }
      }

      private func setUpCaptureSessionInput() {
        weak var weakSelf = self
        sessionQueue.async {
          guard let strongSelf = weakSelf else {
            print("Self is nil!")
            return
          }
          let cameraPosition: AVCaptureDevice.Position = strongSelf.isUsingFrontCamera ? .front : .back
          guard let device = strongSelf.captureDevice(forPosition: cameraPosition) else {
            print("Failed to get capture device for camera position: \(cameraPosition)")
            return
          }
          do {
            strongSelf.captureSession.beginConfiguration()
            let currentInputs = strongSelf.captureSession.inputs
            for input in currentInputs {
              strongSelf.captureSession.removeInput(input)
            }

            let input = try AVCaptureDeviceInput(device: device)
            guard strongSelf.captureSession.canAddInput(input) else {
              print("Failed to add capture session input.")
              return
            }
            strongSelf.captureSession.addInput(input)
            strongSelf.captureSession.commitConfiguration()
          } catch {
            print("Failed to create capture device input: \(error.localizedDescription)")
          }
        }
      }

      private func startSession() {
        weak var weakSelf = self
        sessionQueue.async {
          guard let strongSelf = weakSelf else {
            print("Self is nil!")
            return
          }
          strongSelf.captureSession.startRunning()
        }
      }

  private func stopSession() {
    weak var weakSelf = self
    sessionQueue.async {
      guard let strongSelf = weakSelf else {
        print("Self is nil!")
        return
      }
      strongSelf.captureSession.stopRunning()
    }
  }

  private func setUpPreviewOverlayView() {
    self.view.addSubview(previewOverlayView)
    NSLayoutConstraint.activate([
      previewOverlayView.centerXAnchor.constraint(equalTo: self.view.centerXAnchor),
      previewOverlayView.centerYAnchor.constraint(equalTo: self.view.centerYAnchor),
      previewOverlayView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
      previewOverlayView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),

    ])
  }

  private func setUpAnnotationOverlayView() {
    self.view.addSubview(annotationOverlayView)
    NSLayoutConstraint.activate([
      annotationOverlayView.topAnchor.constraint(equalTo: self.view.topAnchor),
      annotationOverlayView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
      annotationOverlayView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
      annotationOverlayView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor),
    ])
  }

  private func captureDevice(forPosition position: AVCaptureDevice.Position) -> AVCaptureDevice? {
    if #available(iOS 10.0, *) {
      let discoverySession = AVCaptureDevice.DiscoverySession(
        deviceTypes: [.builtInWideAngleCamera],
        mediaType: .video,
        position: .unspecified
      )
      return discoverySession.devices.first { $0.position == position }
    }
    return nil
  }

  private func removeDetectionAnnotations() {
    for annotationView in annotationOverlayView.subviews {
      annotationView.removeFromSuperview()
    }
  }

  private func updatePreviewOverlayViewWithLastFrame() {
    guard let lastFrame = lastFrame,
      let imageBuffer = CMSampleBufferGetImageBuffer(lastFrame)
    else {
      return
    }
    self.updatePreviewOverlayViewWithImageBuffer(imageBuffer)
    self.removeDetectionAnnotations()
  }

  private func updatePreviewOverlayViewWithImageBuffer(_ imageBuffer: CVImageBuffer?) {
    guard let imageBuffer = imageBuffer else {
      return
    }
    let orientation: UIImage.Orientation = isUsingFrontCamera ? .leftMirrored : .right
    let image = UIUtilities.createUIImage(from: imageBuffer, orientation: orientation)
    previewOverlayView.image = image
  }

  private func convertedPoints(
    from points: [NSValue]?,
    width: CGFloat,
    height: CGFloat
  ) -> [NSValue]? {
    return points?.map {
      let cgPointValue = $0.cgPointValue
      let normalizedPoint = CGPoint(x: cgPointValue.x / width, y: cgPointValue.y / height)
      let cgPoint = previewLayer.layerPointConverted(fromCaptureDevicePoint: normalizedPoint)
      let value = NSValue(cgPoint: cgPoint)
      return value
    }
  }

  private func normalizedPoint(
    fromVisionPoint point: VisionPoint,
    width: CGFloat,
    height: CGFloat
  ) -> CGPoint {
    let cgPoint = CGPoint(x: point.x, y: point.y)
    var normalizedPoint = CGPoint(x: cgPoint.x / width, y: cgPoint.y / height)
    normalizedPoint = previewLayer.layerPointConverted(fromCaptureDevicePoint: normalizedPoint)
    return normalizedPoint
  }

  private func rotate(_ view: UIView, orientation: UIImage.Orientation) {
    var degree: CGFloat = 0.0
    switch orientation {
    case .up, .upMirrored:
      degree = 90.0
    case .rightMirrored, .left:
      degree = 180.0
    case .down, .downMirrored:
      degree = 270.0
    case .leftMirrored, .right:
      degree = 0.0
    }
    view.transform = CGAffineTransform.init(rotationAngle: degree * 3.141592654 / 180)
  }
}

// MARK: AVCaptureVideoDataOutputSampleBufferDelegate

extension CameraViewController: AVCaptureVideoDataOutputSampleBufferDelegate {

  func captureOutput(
    _ output: AVCaptureOutput,
    didOutput sampleBuffer: CMSampleBuffer,
    from connection: AVCaptureConnection
  ) {
    guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
      print("Failed to get image buffer from sample buffer.")
      return
    }

    lastFrame = sampleBuffer
    let visionImage = VisionImage(buffer: sampleBuffer)
    let orientation = UIUtilities.imageOrientation(
      fromDevicePosition: isUsingFrontCamera ? .front : .back
    )
    visionImage.orientation = orientation

    guard let inputImage = MLImage(sampleBuffer: sampleBuffer) else {
      print("Failed to create MLImage from sample buffer.")
      return
    }
    inputImage.orientation = orientation

    let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
    let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))
    var shouldEnableClassification = false
    var shouldEnableMultipleObjects = false

    scanBarcodesOnDevice(in: visionImage, width: imageWidth, height: imageHeight)
  }
}

// MARK: - Constants
private enum Constant {
  static let alertControllerTitle = "Vision Detectors"
  static let alertControllerMessage = "Select a detector"
  static let cancelActionTitleText = "Cancel"
  static let videoDataOutputQueueLabel = "com.google.mlkit.visiondetector.VideoDataOutputQueue"
  static let sessionQueueLabel = "com.google.mlkit.visiondetector.SessionQueue"
  static let noResultsMessage = "No Results"
  static let localModelFile = (name: "bird", type: "tflite")
  static let labelConfidenceThreshold = 0.75
  static let smallDotRadius: CGFloat = 4.0
  static let lineWidth: CGFloat = 3.0
  static let originalScale: CGFloat = 1.0
  static let padding: CGFloat = 10.0
  static let resultsLabelHeight: CGFloat = 200.0
  static let resultsLabelLines = 5
  static let imageLabelResultFrameX = 0.4
  static let imageLabelResultFrameY = 0.1
  static let imageLabelResultFrameWidth = 0.5
  static let imageLabelResultFrameHeight = 0.8
  static let segmentationMaskAlpha: CGFloat = 0.5
}
