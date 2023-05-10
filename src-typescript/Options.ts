import { IOptions } from './Interface';

export const defaultOptions: Required<IOptions> = Object.freeze({
  barcodeFormats: {
    Code128: true,
    Code39: true,
    Code93: true,
    CodaBar: true,
    DataMatrix: true,
    EAN13: true,
    EAN8: true,
    ITF: true,
    QRCode: true,
    UPCA: true,
    UPCE: true,
    PDF417: true,
    Aztec: true,
  },
  beepOnSuccess: false,
  vibrateOnSuccess: false,
  detectorSize: 0.6,
  detectorAspectRatio: "1:1",
  drawFocusRect: true,
  focusRectColor: "#FFFFFF",
  focusRectBorderRadius: 100,
  focusRectBorderThickness: 5,
  drawFocusLine: false,
  focusLineColor: "#ff2d37",
  focusLineThickness: 2,
  drawFocusBackground: false,
  focusBackgroundColor: "#66FFFFFF",
  stableThreshold: 5,
  debugOverlay: false
});
