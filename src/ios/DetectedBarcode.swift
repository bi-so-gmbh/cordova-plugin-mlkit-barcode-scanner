import MLKit

class DetectedBarcode: Hashable, Equatable, CustomDebugStringConvertible {

    var debugDescription: String {
        var des: String = "\(type(of: self)) {"
        for child in Mirror(reflecting: self).children {
            if let propName = child.label {
                des += "\n\t\(propName): \(child.value)"
            }
        }

        return des + "\n}"
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(value)
        hasher.combine(barcodeType)
        hasher.combine(format)
        let _ = hasher.finalize()
    }

    static func == (lhs: DetectedBarcode, rhs: DetectedBarcode) -> Bool {
        return lhs.value.elementsEqual(rhs.value) && lhs.barcodeType == rhs.barcodeType && lhs.format == rhs.format
    }

    public private(set) var bounds: CGRect
    public private(set) var value: String
    public private(set) var format: Int
    public private(set) var barcodeType: Int
    public private(set) var distanceToCenter: CGFloat

    init(barcode: Barcode, bounds: CGRect, centerX: CGFloat, centerY: CGFloat) {
        format = barcode.format.rawValue
        barcodeType = barcode.valueType.rawValue
        self.bounds = bounds
        if let rawValue = barcode.rawValue {
            value = rawValue
        } else {
            value = String(data: barcode.rawData!, encoding: .ascii)!;
        }

        distanceToCenter = hypot((centerX - bounds.midX), (centerY - bounds.midY));
    }

    public func isInScanArea(scanArea: CGRect) -> Bool{
        return scanArea.contains(CGPoint(x: bounds.midX, y: bounds.midY))
    }

    public func outputAsDictionary() -> [String: Any] {
        return ["value": value, "type": barcodeType, "format": format, "distanceToCenter": distanceToCenter]
    }
}
