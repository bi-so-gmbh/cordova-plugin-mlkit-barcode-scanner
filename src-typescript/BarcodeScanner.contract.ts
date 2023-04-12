import { MLKitBarcodeScanner } from './BarcodeScanner.plugin';

declare global {
  interface CordovaPlugins {
    mlkit: {
      barcodeScanner: MLKitBarcodeScanner;
    };
  }
}

export { MLKitBarcodeScanner } from './BarcodeScanner.plugin';
export { IBarcodeFormats, IError, IOptions, IResult } from './Interface';
