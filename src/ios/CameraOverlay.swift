import AVFoundation
import CoreVideo
import MLKit
import UIKit

class CameraOverlay: UIView {

    var previewOverlayView: UIImageView
    private var lastFrame: CMSampleBuffer?
    var previewLayer: AVCaptureVideoPreviewLayer!
    private var scanArea: CGRect
    private var settings: ScannerSettings

    init(settings: ScannerSettings, parentView: UIView, previewLayer: AVCaptureVideoPreviewLayer) {
        print("CameraOverlayInit")
        previewOverlayView = UIImageView(frame: .zero)
        previewOverlayView.contentMode = UIView.ContentMode.scaleAspectFill
        previewOverlayView.translatesAutoresizingMaskIntoConstraints = false

        parentView.addSubview(previewOverlayView)
        NSLayoutConstraint.activate([
            previewOverlayView.centerXAnchor.constraint(equalTo: parentView.centerXAnchor),
            previewOverlayView.centerYAnchor.constraint(equalTo: parentView.centerYAnchor),
            previewOverlayView.leadingAnchor.constraint(equalTo: parentView.leadingAnchor),
            previewOverlayView.trailingAnchor.constraint(equalTo: parentView.trailingAnchor),
        ])

        self.scanArea = Utils.calculateCGRect(height: previewOverlayView.frame.height, width: previewOverlayView.frame.width, scaleFactor: settings.detectorSize, aspectRatio: settings.aspectRatioF)
        self.previewLayer = previewLayer
        self.settings = settings

        super.init(frame: .zero)

        self.translatesAutoresizingMaskIntoConstraints = false

        parentView.addSubview(self)
        NSLayoutConstraint.activate([
            self.topAnchor.constraint(equalTo: parentView.topAnchor),
            self.leadingAnchor.constraint(equalTo: parentView.leadingAnchor),
            self.trailingAnchor.constraint(equalTo: parentView.trailingAnchor),
            self.bottomAnchor.constraint(equalTo: parentView.bottomAnchor),
        ])

        self.isOpaque = false
        self.backgroundColor = UIColor.clear

        print("CameraOverlayInit-end")
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func draw(_ rect: CGRect) {
        print(self.bounds)
        self.scanArea = Utils.calculateCGRect(height: bounds.height, width: bounds.width, scaleFactor: settings.detectorSize, aspectRatio: settings.aspectRatioF)
        if let context = UIGraphicsGetCurrentContext() {
            drawScanArea(context: context)
        }
    }

    private func removeDetectionAnnotations() {
        for annotationView in self.subviews {
            annotationView.removeFromSuperview()
        }
    }

    public func updatePreviewOverlayViewWithLastFrame() {
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
        let image = UIUtilities.createUIImage(from: imageBuffer, orientation: .right)
        previewOverlayView.image = image
    }

    public func updateLastFrame(lastFrame:CMSampleBuffer) {
        self.lastFrame = lastFrame
    }

    public func drawRectangle(_ rectangle: CGRect, color: UIColor, cornerRadius: Int = 0, borderThickness: Int = 5) {
        guard rectangle.isValid() else { return }
        let rectangleView = UIView(frame: rectangle)
        rectangleView.layer.cornerRadius = CGFloat(cornerRadius)
        rectangleView.layer.borderColor = color.cgColor
        rectangleView.layer.borderWidth = CGFloat(borderThickness)
        addSubview(rectangleView)
    }

    private func drawScanArea(context: CGContext) {
        if (settings.drawFocusBackground){
            drawFocusBackground(context: context, color: settings.focusBackgroundUIColor, radius: settings.focusRectBorderRadius)
        }
        if (settings.drawFocusLine) {
            drawFocusLine(context: context, color: settings.focusLineUIColor, thickness: settings.focusLineThickness)
        }
        if (settings.drawFocusRect) {
            drawScanAreaOutline(context: context, color: settings.focusRectUIColor, thickness: settings.focusRectBorderThickness, radius: settings.focusRectBorderRadius)
        }
    }

    private func drawFocusLine(context: CGContext, color: UIColor, thickness: Int) {
        context.setLineWidth(CGFloat(thickness))
        context.setStrokeColor(color.cgColor)
        context.beginPath()
        context.move(to: CGPointMake(scanArea.minX, CGFloat(bounds.height/2)))
        context.addLine(to: CGPointMake(scanArea.maxX, CGFloat(bounds.height/2)))
        context.strokePath()
        context.saveGState()
    }

    private func drawScanAreaOutline(context: CGContext, color: UIColor, thickness: Int, radius: Int) {
        var rounded = UIBezierPath(roundedRect: scanArea, cornerRadius: CGFloat(radius))
        context.addPath(rounded.cgPath)
        context.setLineWidth(CGFloat(thickness))
        context.setStrokeColor(color.cgColor)
        context.strokePath()
        context.saveGState()
    }

    private func drawFocusBackground(context: CGContext, color: UIColor, radius: Int) {
        var rounded = UIBezierPath(roundedRect: scanArea, cornerRadius: CGFloat(radius))
        color.setFill()
        context.fill(bounds)
        context.saveGState()
        context.setBlendMode(.destinationOut)
        context.addPath(rounded.cgPath)
        context.fillPath()
        context.saveGState()
        context.setBlendMode(.normal)
    }
}
