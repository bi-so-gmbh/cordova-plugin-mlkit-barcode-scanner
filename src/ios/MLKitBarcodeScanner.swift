@objc(MLKitBarcodeScanner)
class MLKitBarcodeScanner: CDVPlugin, CameraViewControllerDelegate {

    private var callbackId:String?


    @objc(startScan:)
    func startScan(command: CDVInvokedUrlCommand) {
        self.callbackId = command.callbackId
        var options:[String: Any] = [String: Any]()
        if (!command.arguments.isEmpty) {
            let first = command.arguments!.first as? [String: Any]
            options = first ?? options
        }
        var settings:ScannerSettings = ScannerSettings(options: options)

        let cameraViewController = CameraViewController(settings: settings)
        cameraViewController.delegate = self

        self.viewController.present(cameraViewController, animated: false)
    }

    func onComplete(result: String) {
        self.viewController.dismiss(animated: false)
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK, messageAs: [result, "test", "test"]
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: self.callbackId
        )
    }

    func onError(error: String) {
        self.viewController.dismiss(animated: false)
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR, messageAs: [error]
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: self.callbackId
        )
    }
}
