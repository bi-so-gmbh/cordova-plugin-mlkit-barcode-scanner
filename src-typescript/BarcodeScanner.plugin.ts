import {barcodeFormat, barcodeType} from './Detector';
import {
  IBarcodeFormats,
  IConfig,
  IError,
  IOptions,
  IResult,
  IPrettyResult
} from './Interface';
import {defaultOptions} from './Options';
import {keyByValue} from './util/Object';

export class MLKitBarcodeScanner {
  private getBarcodeFormat(format: number): string {
    return keyByValue(barcodeFormat, format);
  }

  private getBarcodeType(type: number): string {
    return keyByValue(barcodeType, type);
  }

  private prettyPrintBarcode(barcode: IResult): IPrettyResult {
        return  {
            "value": barcode.value,
            "type" : this.getBarcodeType(barcode.type),
            "format": this.getBarcodeFormat(barcode.format),
            "distanceToCenter": Math.round(barcode.distanceToCenter * 100) / 100
        }
    }

  private getBarcodeFormatFlags(barcodeFormats?: IBarcodeFormats): number {
    let barcodeFormatFlag = 0;
    let key: keyof typeof barcodeFormat;
    const formats = barcodeFormats || defaultOptions.barcodeFormats;

    // eslint-disable-next-line no-restricted-syntax
    for (key in formats) {
      if (
          barcodeFormat.hasOwnProperty(key) &&
          formats.hasOwnProperty(key) &&
          formats[key]
      ) {
        barcodeFormatFlag += barcodeFormat[key];
      }
    }
    return barcodeFormatFlag;
  }

  scan(
      userOptions: IOptions,
      success: (result: IPrettyResult[]) => unknown,
      failure: (error: IError) => unknown,
  ): void {
    const barcodeFormats =
        userOptions?.barcodeFormats || defaultOptions.barcodeFormats;
    const config: IConfig = {
      ...defaultOptions,
      ...userOptions,
      barcodeFormats: this.getBarcodeFormatFlags(barcodeFormats),
    };

    this.sendScanRequest(config, success, failure);
  }

  private sendScanRequest(
      config: IConfig,
      successCallback: (result: IPrettyResult[]) => unknown,
      failureCallback: (error: IError) => unknown,
  ): void {
    cordova.exec(
        (data: [IResult]) => {
          successCallback(data.map((b) => this.prettyPrintBarcode(b)));
        },
        (err: (string | null)) => {
          switch (err) {
            case 'NO_CAMERA_PERMISSION':
            case 'NO_CAMERA':
              failureCallback({
                cancelled: true,
                message: err
              });
              break;
            case 'JSON_EXCEPTION':
            default:
              failureCallback({
                cancelled: false,
                message: err
              });
              break;
          }
        },
        'cordova-plugin-mlkit-barcode-scanner',
        'startScan',
        [config],
    );
  }
}

const barcodeScanner = new MLKitBarcodeScanner();
module.exports = barcodeScanner;
