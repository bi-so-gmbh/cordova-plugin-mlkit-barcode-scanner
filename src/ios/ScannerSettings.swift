
class ScannerSettings: CustomDebugStringConvertible {
  var debugDescription: String {
    var des: String = "\(type(of: self)) {"
    for child in Mirror(reflecting: self).children {
      if let propName = child.label {
        des += "\n\t\(propName): \(child.value)"
      }
    }

    return des + "\n}"
  }

  // on android lines are much thinner, this evens out the look and feel
  private static let LINE_THICKNESS_ADJUSTMENT = -3
  // same with corner radius, way less pronounced on android
  private static var CORNER_RADIUS_ADJUSTMENT = -5

  public private(set) var barcodeFormats: Int = 1234
  public private(set) var aspectRatio: String = "1:1"
  public private(set) var aspectRatioF: Float
  public private(set) var detectorSize: Double = 0.5
  public private(set) var drawFocusRect: Bool = true
  public private(set) var focusRectColor: String = "#FFFFFF"
  public private(set) var focusRectUIColor: UIColor
  public private(set) var focusRectBorderRadius: Int = 100
  public private(set) var focusRectBorderThickness: Int = 2
  public private(set) var drawFocusLine: Bool = true
  public private(set) var focusLineColor: String = "#FFFFFF"
  public private(set) var focusLineUIColor: UIColor
  public private(set) var focusLineThickness: Int = 1
  public private(set) var drawFocusBackground: Bool = true
  public private(set) var focusBackgroundColor: String = "#CCFFFFFF"
  public private(set) var focusBackgroundUIColor: UIColor
  public private(set) var beepOnSuccess: Bool = false
  public private(set) var vibrateOnSuccess: Bool = false
  public private(set) var stableThreshold: Int = 5
  public private(set) var debugOverlay: Bool = false
  public private(set) var ignoreRotatedBarcodes: Bool = false

  init(options:[String:Any]) {
    for (key, value) in options {
      switch (key) {
      case Settings.BARCODE_FORMATS:
        if let temp = Utils.getInt(input: value) {
          barcodeFormats = temp
        }
        break
      case Settings.DETECTOR_ASPECT_RATIO:
        if let temp = value as? String {
          aspectRatio = temp
        }
        break
      case Settings.DETECTOR_SIZE:
        if let temp = Utils.getDouble(input: value) {
          detectorSize = temp
        }
        break
      case Settings.DRAW_FOCUS_RECT:
        if let temp = value as? Bool {
          drawFocusRect = temp
        }
        break
      case Settings.FOCUS_RECT_COLOR:
        if let temp = value as? String {
          focusRectColor = temp
        }
        break
      case Settings.FOCUS_RECT_BORDER_RADIUS:
        if let temp = Utils.getInt(input: value) {
          focusRectBorderRadius = max(1, temp + Self.CORNER_RADIUS_ADJUSTMENT)
        }
        break
      case Settings.FOCUS_RECT_BORDER_THICKNESS:
        if let temp = Utils.getInt(input: value) {
          focusRectBorderThickness = max(1, temp + Self.LINE_THICKNESS_ADJUSTMENT)
        }
        break
      case Settings.DRAW_FOCUS_LINE:
        if let temp = value as? Bool {
          drawFocusLine = temp
        }
        break
      case Settings.FOCUS_LINE_COLOR:
        if let temp = value as? String {
          focusLineColor = temp
        }
        break
      case Settings.FOCUS_LINE_THICKNESS:
        if let temp = Utils.getInt(input: value) {
          focusLineThickness = max(1, temp + Self.LINE_THICKNESS_ADJUSTMENT)
        }
        break
      case Settings.DRAW_FOCUS_BACKGROUND:
        if let temp = value as? Bool {
          drawFocusBackground = temp
        }
        break
      case Settings.FOCUS_BACKGROUND_COLOR:
        if let temp = value as? String {
          focusBackgroundColor = temp
        }
        break
      case Settings.BEEP_ON_SUCCESS:
        if let temp = value as? Bool {
          beepOnSuccess = temp
        }
        break
      case Settings.VIBRATE_ON_SUCCESS:
        if let temp = value as? Bool {
          vibrateOnSuccess = temp
        }
        break
      case Settings.STABLE_THRESHOLD:
        if let temp = Utils.getInt(input: value) {
          stableThreshold = temp
        }
        break
      case Settings.DEBUG_OVERLAY:
        if let temp = value as? Bool {
          debugOverlay = temp
        }
        break
      case Settings.IGNORE_ROTATED_BARCODES:
        if let temp = value as? Bool {
          ignoreRotatedBarcodes = temp
        }
        break
      default:
        print("Unknown option: \(key)")
      }
    }

    focusRectUIColor = Utils.hexStringToUIColor(hex: focusRectColor)
    focusLineUIColor = Utils.hexStringToUIColor(hex: focusLineColor)
    focusBackgroundUIColor = Utils.hexStringToUIColor(hex: focusBackgroundColor)
    aspectRatioF = Utils.getAspectRatioFromString(aspectRatioString: aspectRatio)
  }

  convenience init() {
    self.init(options: [:])
  }
}

private enum Settings {
  static let BARCODE_FORMATS: String = "barcodeFormats";
  static let DETECTOR_ASPECT_RATIO: String = "detectorAspectRatio";
  static let DETECTOR_SIZE: String = "detectorSize";
  static let DRAW_FOCUS_RECT: String = "drawFocusRect";
  static let FOCUS_RECT_COLOR: String = "focusRectColor";
  static let FOCUS_RECT_BORDER_RADIUS: String = "focusRectBorderRadius";
  static let FOCUS_RECT_BORDER_THICKNESS: String = "focusRectBorderThickness";
  static let DRAW_FOCUS_LINE: String = "drawFocusLine";
  static let FOCUS_LINE_COLOR: String = "focusLineColor";
  static let FOCUS_LINE_THICKNESS: String = "focusLineThickness";
  static let DRAW_FOCUS_BACKGROUND: String = "drawFocusBackground";
  static let FOCUS_BACKGROUND_COLOR: String = "focusBackgroundColor";
  static let BEEP_ON_SUCCESS: String = "beepOnSuccess";
  static let VIBRATE_ON_SUCCESS: String = "vibrateOnSuccess";
  static let STABLE_THRESHOLD: String = "stableThreshold";
  static let DEBUG_OVERLAY: String = "debugOverlay";
  static let IGNORE_ROTATED_BARCODES: String = "ignoreRotatedBarcodes";
}
