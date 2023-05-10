import MLImage
import MLKit

protocol BarcodesListener: NSObjectProtocol {
    func onBarcodesFound(_ barcodes: [DetectedBarcode])
}

class BarcodeAnalyzer {

    private var scanner: BarcodeScanner
    private var cameraOverlay: CameraOverlay
    private var barcodesListener: BarcodesListener
    private var settings: ScannerSettings

    private var lastBarcodes: [DetectedBarcode] = []
    private var stableCounter: Int = 0

    init(settings: ScannerSettings, barcodesListener: BarcodesListener, cameraOverlay:CameraOverlay) {
        var useBarcodeFormats = settings.barcodeFormats
        if (useBarcodeFormats == 0 || useBarcodeFormats == 1234) {
            useBarcodeFormats = BarcodeFormat.code39.rawValue | BarcodeFormat.dataMatrix.rawValue;
        }
        let barcodeFormats = BarcodeFormat(rawValue: useBarcodeFormats)
        scanner = BarcodeScanner.barcodeScanner(options: BarcodeScannerOptions(formats: barcodeFormats))
        self.cameraOverlay = cameraOverlay
        self.barcodesListener = barcodesListener
        self.settings = settings
    }

    func analyze(in image: VisionImage, width: CGFloat, height: CGFloat) {
        var barcodes: [Barcode] = []
        do {
            barcodes = try scanner.results(in: image)
        } catch let error {
            print(error.localizedDescription)
        }

        var detectedBarcodes: [DetectedBarcode] = []
        for barcode in barcodes {
            let normalizedRect = CGRect(
                x: barcode.frame.origin.x / width,
                y: barcode.frame.origin.y / height,
                width: barcode.frame.size.width / width,
                height: barcode.frame.size.height / height
            )
            let convertedRect = cameraOverlay.previewLayer.layerRectConverted(fromMetadataOutputRect: normalizedRect)
            detectedBarcodes.append(DetectedBarcode(barcode: barcode, bounds: convertedRect, centerX: cameraOverlay.previewLayer.bounds.midX, centerY: cameraOverlay.previewLayer.bounds.midY))
        }

        if(settings.debugOverlay) {
            cameraOverlay.drawDebugOverlay(barcodes: detectedBarcodes)
        }

        if (areBarcodesStable(barcodes: detectedBarcodes) && stableCounter >= settings.stableThreshold) {
            var barcodesInScanArea: [DetectedBarcode] = []
            for barcode in detectedBarcodes {
                if(barcode.isInScanArea(scanArea: cameraOverlay.scanArea)) {
                    barcodesInScanArea.append(barcode)
                }
            }
            barcodesInScanArea.sort {
                $0.distanceToCenter < $1.distanceToCenter
            }
            if (!barcodesInScanArea.isEmpty) {
                barcodesListener.onBarcodesFound(barcodesInScanArea)
            }
        }
    }

    private func areBarcodesStable(barcodes: [DetectedBarcode]) -> Bool {
        let barcodesSet = Set(barcodes)
        let lastBarcodesSet = Set(lastBarcodes)
        let differences = barcodesSet.subtracting(lastBarcodesSet)
        if (!barcodes.isEmpty && differences.isEmpty) {
            stableCounter += 1
            print("barcodes stable for \(stableCounter)/\(settings.stableThreshold)")
            return true
        }
        stableCounter = 0
        lastBarcodes = barcodes
        return false
    }
}
