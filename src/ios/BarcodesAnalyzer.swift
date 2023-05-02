import MLImage
import MLKit

protocol BarcodesListener: NSObjectProtocol {
    func onBarcodesFound(barcodes: Array<Barcode>)
}

class BarcodeAnalyzer {

    private var scanner: BarcodeScanner
    private var cameraOverlay: CameraOverlay
    private var barcodesListener: BarcodesListener
    private var scannerSettings: ScannerSettings

    init(settings: ScannerSettings, barcodesListener: BarcodesListener, cameraOverlay:CameraOverlay) {
        print("BarcodeAnalyzerInit")
        var useBarcodeFormats = settings.barcodeFormats
        if (useBarcodeFormats == 0 || useBarcodeFormats == 1234) {
            useBarcodeFormats = BarcodeFormat.code39.rawValue | BarcodeFormat.dataMatrix.rawValue;
        }
        let barcodeFormats = BarcodeFormat(rawValue: useBarcodeFormats)
        scanner = BarcodeScanner.barcodeScanner(options: BarcodeScannerOptions(formats: barcodeFormats))
        self.cameraOverlay = cameraOverlay
        self.barcodesListener = barcodesListener
        self.scannerSettings = settings
        print("BarcodeAnalyzerInit-end")
    }

    func analyze(in image: VisionImage, width: CGFloat, height: CGFloat) {
        var barcodes: [Barcode] = []
        do {
            barcodes = try scanner.results(in: image)
        } catch let error {
            print(error.localizedDescription)
        }
        weak var weakSelf = self
        DispatchQueue.main.sync {
            guard weakSelf != nil else {
                return
            }
            cameraOverlay.updatePreviewOverlayViewWithLastFrame()

            guard !barcodes.isEmpty else {
                return
            }
            print("analyze")
            for barcode in barcodes {
                let normalizedRect = CGRect(
                    x: barcode.frame.origin.x / width,
                    y: barcode.frame.origin.y / height,
                    width: barcode.frame.size.width / width,
                    height: barcode.frame.size.height / height
                )
                let convertedRect = cameraOverlay.previewLayer.layerRectConverted(
                    fromMetadataOutputRect: normalizedRect
                )
                if(scannerSettings.debugOverlay) {
                    cameraOverlay.drawRectangle(convertedRect, color: UIColor.green)
                }
            }
            barcodesListener.onBarcodesFound(barcodes: barcodes)
        }
    }

}
