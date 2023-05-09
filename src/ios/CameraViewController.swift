import UIKit
import SwiftUI
import AVFoundation
import MLImage
import MLKit

protocol CameraViewControllerDelegate: NSObjectProtocol {
    func onComplete(_ result: [DetectedBarcode])
    func onError(_ error: String)
}

class CameraViewController: UIViewController, BarcodesListener {
    weak var delegate: CameraViewControllerDelegate?
    private let captureSession = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "sessionQueue")
    private var previewLayer = AVCaptureVideoPreviewLayer()
    private var settings: ScannerSettings
    private var cameraOverlay: CameraOverlay!
    private var barcodeAnalyzer : BarcodeAnalyzer!

    func onBarcodesFound(_ barcodes: [DetectedBarcode]) {
        finishWithResult(barcodes)
    }

    init(settings: ScannerSettings) {
        self.settings = settings
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        self.settings = ScannerSettings()
        super.init(coder: coder)
    }

  override func viewDidLoad() {
      cameraOverlay = CameraOverlay(settings: settings, parentView: view)
      barcodeAnalyzer = BarcodeAnalyzer(settings: settings, barcodesListener: self, cameraOverlay: cameraOverlay)

      if AVCaptureDevice.authorizationStatus(for: .video) == .authorized {
          setupCaptureSession()
          setupUI()
      } else {
          AVCaptureDevice.requestAccess(for: .video, completionHandler: { (authorized) in
              DispatchQueue.main.async {
                  if authorized {
                      self.setupCaptureSession()
                      self.setupUI()
                  } else {
                      self.finishWithError("NO_CAMERA_PERMISSION")
                  }
              }
          })
      }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }

    override func viewWillAppear(_ animated: Bool) {
        startSession()
        while(!captureSession.isRunning) {
            usleep(100)
        }
        usleep(150000)
    }

    override func viewDidDisappear(_ animated: Bool) {
        stopSession()
    }

    override func willTransition(to newCollection: UITraitCollection, with coordinator: UIViewControllerTransitionCoordinator) {
        previewLayer.frame = CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)
        cameraOverlay.setPreviewLayer(previewlayer: previewLayer)

        switch UIDevice.current.orientation {
            case UIDeviceOrientation.portraitUpsideDown:
                previewLayer.connection?.videoOrientation = .portraitUpsideDown

            case UIDeviceOrientation.landscapeLeft:
                previewLayer.connection?.videoOrientation = .landscapeRight

            case UIDeviceOrientation.landscapeRight:
                previewLayer.connection?.videoOrientation = .landscapeLeft

            case UIDeviceOrientation.portrait:
                previewLayer.connection?.videoOrientation = .portrait

            default:
                break
        }
    }

    private func setupCaptureSession() {
           guard let videoDevice = captureDevice() else {
               finishWithError("NO_CAMERA")
               return

           }
        guard let videoDeviceInput = try? AVCaptureDeviceInput(device: videoDevice) else {
            finishWithError("NO_CAMERA")
            return

        }
           guard captureSession.canAddInput(videoDeviceInput) else { return }

           captureSession.addInput(videoDeviceInput)

           previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.backgroundColor = UIColor.black.cgColor
           previewLayer.frame = view.bounds
           previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill

           previewLayer.connection?.videoOrientation = .portrait

           let output = AVCaptureVideoDataOutput()
           output.alwaysDiscardsLateVideoFrames = true
           output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "sampleQueue"))

           captureSession.addOutput(output)
    }

    private func setupUI() {
        view.layer.addSublayer(previewLayer)
         cameraOverlay.setPreviewLayer(previewlayer: previewLayer)
         view.bringSubviewToFront(cameraOverlay)

        var torchButton = UIButton(type: UIButton.ButtonType.custom)
        torchButton.backgroundColor = UIColor.white
        torchButton.layer.cornerRadius = 35
        torchButton.frame = CGRect(x: view.frame.width - 70, y: view.frame.height - 110, width: 70, height: 70)
        torchButton.clipsToBounds = true
        if let image = UIImage(named: "flashlight.png")
        {
            torchButton.setImage(image, for: UIControl.State.normal)
        }

        view.addSubview(torchButton)
        view.bringSubviewToFront(torchButton)
    }

    private func captureDevice() -> AVCaptureDevice? {
        if #available(iOS 10.0, *) {
            let discoverySession = AVCaptureDevice.DiscoverySession(
                deviceTypes: [.builtInWideAngleCamera],
                mediaType: .video,
                position: .unspecified
            )
            return discoverySession.devices.first { $0.position == .back }
        }
        return nil
    }

    private func startSession() {
        weak var weakSelf = self
        sessionQueue.async {
            guard let strongSelf = weakSelf else {return}
            strongSelf.captureSession.startRunning()
        }
    }

    private func stopSession() {
        weak var weakSelf = self
        sessionQueue.async {
            guard let strongSelf = weakSelf else {return}
            strongSelf.captureSession.stopRunning()
        }
    }

    private func finishWithError(_ error: String) {
        delegate?.onError(error)
    }

    private func finishWithResult(_ result: [DetectedBarcode]){
        delegate?.onComplete(result)
    }
}

extension CameraViewController: AVCaptureVideoDataOutputSampleBufferDelegate {

    func captureOutput(_ output: AVCaptureOutput,didOutput sampleBuffer: CMSampleBuffer,from connection: AVCaptureConnection) {
            guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
                print("Failed to get image buffer from sample buffer.")
                return
            }

            let visionImage = VisionImage(buffer: sampleBuffer)
            let orientation = Utils.imageOrientation(fromDevicePosition: .back)
            visionImage.orientation = orientation

            guard let inputImage = MLImage(sampleBuffer: sampleBuffer) else {
                print("Failed to create MLImage from sample buffer.")
                return
            }
            inputImage.orientation = orientation

            let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
            let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))

            barcodeAnalyzer.analyze(in: visionImage, width: imageWidth, height: imageHeight)
    }
}
