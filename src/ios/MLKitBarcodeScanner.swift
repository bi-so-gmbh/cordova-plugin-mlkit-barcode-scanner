@objc(MLKitBarcodeScanner)
class MLKitBarcodeScanner: CDVPlugin {
    @objc(startScan:)
  func startScan(command: CDVInvokedUrlCommand) {
    var pluginResult = CDVPluginResult(
      status: CDVCommandStatus_OK, messageAs: "Test"
    )

    let cameraViewController = CameraViewController()

    self.viewController.present(cameraViewController, animated: false)

    /*self.commandDelegate!.send(
      pluginResult,
      callbackId: command.callbackId
    )*/
  }
}
