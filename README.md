# :camera: cordova-plugin-mlkit-barcode-scanner

## Purpose of this Project

The purpose of this project is to provide a barcode scanner utilizing the Google ML Kit Vision library for the Cordova framework on iOS and Android.
The MLKit library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free.

## Plugin Dependencies

| Dependency                     | Version    | Info                    |
|--------------------------------|------------|-------------------------|
| `cordova`                      | `>=7.1.0`  |                         |
| `cordova-android`              | `>=11.0.0` |                         |
| `cordova-ios`                  | `>=6.2.0`  |                         |

## Installation

Run this command in your cordova folder:

```bash
cordova plugin add github:bi-so-gmbh/cordova-plugin-mlkit-barcode-scanner#v3.0.9
```

This repository uses the tag approach, so if you want a specific version, check out the tags.

## Supported Platforms

- Android
- iOS/iPadOS

## Barcode Support

| 1d formats   | Android | iOS |
|--------------|---------|-----|
| Codabar      | ✓       | ✓   |
| Code 39      | ✓       | ✓   |
| Code 93      | ✓       | ✓   |
| Code 128     | ✓       | ✓   |
| EAN-8.       | ✓       | ✓   |
| EAN-13       | ✓       | ✓   |
| ITF          | ✓       | ✓   |
| MSI          | ✗       | ✗   |
| RSS Expanded | ✗       | ✗   |
| RSS-14       | ✗       | ✗   |
| UPC-A        | ✓       | ✓   |
| UPC-E        | ✓       | ✓   |

| 2d formats  | Android | iOS |
|-------------|---------|-----|
| Aztec       | ✓       | ✓   |
| Codablock   | ✗       | ✗   |
| Data Matrix | ✓       | ✓   |
| MaxiCode    | ✗       | ✗   |
| PDF417      | ✓       | ✓   |
| QR Code     | ✓       | ✓   |

:information_source: Note that this API does not recognize barcodes in these forms:

- 1D Barcodes with only one character
- Barcodes in ITF format with fewer than six characters
- Barcodes encoded with FNC2, FNC3 or FNC4
- QR codes generated in the ECI mode

## Usage

To use the plugin simply call `cordova.plugins.mlkit.barcodeScanner.scan(options, sucessCallback, failureCallback)`. See the sample below.

```javascript
cordova.plugins.mlkit.barcodeScanner.scan(
  options,
  (result) => {
    // Do something with the data
    alert(result);
  },
  (error) => {
    // Error handling
  },
);
```

### Plugin Options

The default options are shown below.
All values are optional.

Note that the `detectorSize` value must be between `0` and `1`, because it determines how many percent of the screen should be covered by the detector.
If the value is greater than 1 the detector will not be visible on the screen.

```javascript
const defaultOptions = {
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
  debugOverlay: false,
  ignoreRotatedBarcodes: false
};
```

### Barcode Detection

All barcodes that are on the image are detected by MLKit. Once the barcodes detected haven't changed for a configurable amount of images (`stableThreshold`) they enter a secondary logic that checks if the barcodes found are in the scan area. Barcodes are considered inside if the center line of the barcode fits into the scan area completely. If the option `ignoreRotatedBarcodes` is set to `true` then barcodes additionally have to have a center line that matches the device rotation. This is done to be able to sort out barcodes rotated by 90 degree.

### Output/Return value

The result of the plugin is a list of detected barcodes, sorted by distanceToCenter with the lowest value being first. DistanceToCenter is calculated by how far away the center of the barcode is from the center of the scan area. I figured the closer the barcode is, the more likely it is that that barcode was supposed to be scanned. 

```javascript
result: [{
  value: string,
  format: string,
  type: string,
  distanceToCenter: number
}]
```

In case the scanner encountered any errors an error object will be returned. The error object property message will contain what amounts to a language key, so it is up to the apps using the plugin to code in a more descriptive error message.

```javascript
error: {
  cancelled: boolean;
  message: string;
}
```

**Current values for message:**
 - NO_CAMERA
 - NO_CAMERA_PERMISSION
 - JSON_EXCEPTION (only used by android)

## Known Issues

**On some devices the camera may be upside down.**

Here is a list of devices with this problem:

- Zebra MC330K (Manufacturer: Zebra Technologies, Model: MC33)

~~Current Solution:
if your device has this problem, you can call the plugin with the option `rotateCamera` set to `true`.
This will rotate the camera stream by 180 degrees.~~
I removed `rotateCamera` because it was causing me a headache and I figured that the device in question isn't used anymore by now.

**Sometimes the orientation of the barcode is detected wrongly**

This is an issue with the fact that MLKit doesn't actually expose the barcode orientation, so any orientation that is used by this plugin basically just checks for the longest side of the barcode bounds. With square barcodes this causes some display issues in the debug overlay, and detection issues with `ignoreRotatedBarcodes` set to `true`. If you expect square barcodes (or qr codes) don't use that setting.

**Barcodes are only read partially**

Another MLKit "feature". Happens especially often on ITF barcodes because the barcode checksum check isn't implemented in MLKit. Should be somewhat mitigated by the detection using the center line of the barcode, but sometimes only parts of the barcode will be read anyway. Only way to deal with it is to play around with `stableThreshold` until you have enough time to get the whole barcode in the scan area.

**Barcode detection doesn't detect all barcodes**

More MLKit issues. When having multiple barcodes on screen, not all of them are detected all the time. If you are wondering why some barcodes are missing, activate `debugOverlay` and you will see which barcodes MLKIt found.

## Development

### Build Process

This project uses npm scripts for building:

```shell
# lint the project using eslint
npm run lint

# removes the generated folders
npm run clean

# build the project (creates www folder)
# (includes clean and lint)
npm run build

# completely removes the platform from the test app and adds it again before running
# sometimes you have to force cordova to re-copy things
npm run cleanRunAndroid
```

A VS Code task for `build` is also included.

## Run the test app

Install cordova:

```
npm i -g cordova
```

Go to test app:

```
cd test/scan-test-app
```

Install node modules:

```
npm i
```

Prepare Cordova:

```
cordova prepare && cordova plugin add ../../ --link --force
```

Build and run the project Android:

```
cordova build android && cordova run android
```

and iOS:

```
cordova build ios && cordova run ios
```

### Versioning

⚠️ Before incrementing the version in `package.json`, remember to increment the version in `plugin.xml` by hand.

### VS Code Extensions

This project is intended to be used with Visual Studio Code and the recommended extensions can be found in [`.vscode/extensions.json`](.vscode/extensions.json).
When you open this repository for the first time in Visual Studio Code you should get a prompt asking you to install the recommended extensions.

(Daenara: Personally, I don't use vscode, so I can't guarantee that is up-to-date)
