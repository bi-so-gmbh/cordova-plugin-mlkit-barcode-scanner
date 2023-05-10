import AudioToolbox
import AVFoundation
import os

@objc(MLKitBarcodeScanner)
class MLKitBarcodeScanner: CDVPlugin, CameraViewControllerDelegate {

    private var callbackId:String?
    private var settings: ScannerSettings!
    private var player: AVAudioPlayer?


    @objc(startScan:)
    func startScan(command: CDVInvokedUrlCommand) {
        self.callbackId = command.callbackId
        var options:[String: Any] = [:]
        if (!command.arguments.isEmpty) {
            let first = command.arguments!.first as? [String: Any]
            options = first ?? options
        }
        settings = ScannerSettings(options: options)

        let cameraViewController = CameraViewController(settings: settings)
        cameraViewController.delegate = self

        self.viewController.present(cameraViewController, animated: true)

    }

    func onComplete(_ result: [DetectedBarcode]) {
        weak var weakSelf = self
        DispatchQueue.main.sync {
            guard weakSelf != nil else {
                return
            }
            self.viewController.dismiss(animated: true)
        }
        if(settings.vibrateOnSuccess) {
            AudioServicesPlayAlertSoundWithCompletion(SystemSoundID(kSystemSoundID_Vibrate)) { }
        }
        if(settings.beepOnSuccess) {
            if let path = Bundle.main.path(forResource: "beep", ofType: "caf")
            {
                do {
                    try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                    try AVAudioSession.sharedInstance().setActive(true)

                    player = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: path), fileTypeHint: AVFileType.caf.rawValue)

                    if let unwrappedPlayer = player {
                        unwrappedPlayer.play()
                    }

                } catch let error {
                    print(error.localizedDescription)
                }
            }
        }
        let output: [[String: Any]] = result.map { $0.outputAsDictionary() }
        let pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK, messageAs: output
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: self.callbackId
        )
    }

    func onError(_ error: String) {
        self.viewController.dismiss(animated: true)
        let pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR, messageAs: error
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: self.callbackId
        )
    }
}
