export interface IBarcodeFormats {
  Aztec: boolean;
  CodaBar: boolean;
  Code39: boolean;
  Code93: boolean;
  Code128: boolean;
  DataMatrix: boolean;
  EAN8: boolean;
  EAN13: boolean;
  ITF: boolean;
  PDF417: boolean;
  QRCode: boolean;
  UPCA: boolean;
  UPCE: boolean;
}

export interface IOptions {
  barcodeFormats?: IBarcodeFormats;
  beepOnSuccess?: boolean;
  vibrateOnSuccess?: boolean;
  detectorSize?: number;
  detectorAspectRatio?: string;
  drawFocusRect?: boolean;
  focusRectColor?: string;
  focusRectBorderRadius?: number;
  focusRectBorderThickness?: number;
  drawFocusLine?: boolean;
  focusLineColor?: string;
  focusLineThickness?: number;
  drawFocusBackground?: boolean;
  focusBackgroundColor?: string;
  stableThreshold?: number;
  debugOverlay?: boolean;
}

export interface IConfig {
  barcodeFormats: number;
  beepOnSuccess?: boolean;
  vibrateOnSuccess?: boolean;
  detectorSize?: number;
  detectorAspectRatio?: string;
  drawFocusRect?: boolean;
  focusRectColor?: string;
  focusRectBorderRadius?: number;
  focusRectBorderThickness?: number;
  drawFocusLine?: boolean;
  focusLineColor?: string;
  focusLineThickness?: number;
  drawFocusBackground?: boolean;
  focusBackgroundColor?: string;
  stableThreshold?: number;
  debugOverlay?: boolean;
}

export interface IResult {
  value: string;
  format: number;
  type: number;
  distanceToCenter: number;
}

export interface IPrettyResult {
  value: string;
  format: string;
  type: string;
  distanceToCenter: number;
}

export interface IError {
  cancelled: boolean;
  message: string;
}
