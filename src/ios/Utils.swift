class Utils {

    /**
     * Calculates a centered rectangle. Rectangle will be centered in the area defined by width x
     * height, have the aspect ratio and have a width of min(width, height) * scaleFactor
     *
     * @param height      height of the area that the rectangle will be centered in
     * @param width       width of the area that the rectangle will be centered in
     * @param scaleFactor factor to scale the rectangle with
     * @param aspectRatio the intended aspect ratio of the rectangle
     * @return rectangle based on float values, centered in the area
     */
    public static func calculateCGRect(height: CGFloat, width: CGFloat, scaleFactor: Double, aspectRatio: Float) -> CGRect {
        //print("size:",height,"x",width)
        let rectWidth:CGFloat  = CGFloat((Double(min(height, width)) * scaleFactor))
        let rectHeight:CGFloat = CGFloat(rectWidth / CGFloat(aspectRatio))

        let offsetX:CGFloat = width - rectWidth
        let offsetY:CGFloat = height - rectHeight

        let left:CGFloat = offsetX / 2
        let top:CGFloat = offsetY / 2

        //return CGRect(origin:CGPoint(x:left, y:top), size: CGSize(width: rectWidth, height: rectHeight))

        return CGRect(x:left, y:top, width:rectWidth, height:rectHeight)
    }

    /**
     * Takes the aspect ratio string and returns it as a fraction.
     *
     * @param aspectRatioString the string containing an aspect ratio like 1:2
     * @return The calculated aspect ratio, or 1 in case of error
     */
    public static func getAspectRatioFromString(aspectRatioString: String) -> Float {
        let separator:String = ":"
        if (aspectRatioString.contains(separator)) {
            let parts:Array<String> = aspectRatioString.components(separatedBy: separator);
            if (parts.count == 2) {
                let left:Float = Float(parts[0]) ?? 1;
                let right:Float = Float(parts[1]) ?? 1;
                return (left / right);
            }
        }
        return 1;
    }

    public static func hexStringToUIColor (hex: String) -> UIColor {

        var cString:String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if (cString.hasPrefix("#")) {
            cString.remove(at: cString.startIndex)
        }

        if (!((cString.count) == 6 || (cString.count) == 8)) {
            return UIColor.gray
        }

        if ((cString.count) == 6) {
            cString = "FF" + cString
        }

        var argbValue:UInt64 = 0
        Scanner(string: cString).scanHexInt64(&argbValue)

        return UIColor(
            red: CGFloat((argbValue & 0x00FF0000) >> 16) / 255.0,
            green: CGFloat((argbValue & 0x0000FF00) >> 8) / 255.0,
            blue: CGFloat(argbValue & 0x000000FF) / 255.0,
            alpha: CGFloat((argbValue & 0xFF000000) >> 24) / 255.0
        )
    }

    public static func getDouble(input:Any) -> Optional<Double> {
        if let temp = input as? NSNumber {
            return temp.doubleValue
        }
        if let temp = input as? NSString {
            return temp.doubleValue
        }
        print("Can't convert \type(of: input) to Double")
        return Optional.none
    }

    public static func getInt(input:Any) -> Optional<Int> {
        if let temp = input as? NSNumber {
            return temp.intValue
        }
        if let temp = input as? NSString {
            return temp.integerValue
        }
        print("Can't convert \type(of: input) to Int")
        return Optional(nil)
    }
}
