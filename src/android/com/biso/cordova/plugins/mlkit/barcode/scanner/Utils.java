package com.biso.cordova.plugins.mlkit.barcode.scanner;

import android.graphics.RectF;

public class Utils {

  /**
   * Calculates a centered rectangle. Rectangle will be centered in the area defined by width x
   * height, have the aspect ratio and have a width of min(width, height) * scaleFactor
   *
   * @param height height of the area that the rectangle will be centered in
   * @param width  width of the area that the rectangle will be centered in
   * @param scaleFactor factor to scale the rectangle with
   * @param aspectRatio the intended aspect ratio of the rectangle
   * @return rectangle based on float values, centered in the area
   */
  public static RectF calculateRectF(int height, int width, double scaleFactor, float aspectRatio) {
    float rectWidth = (float) (Math.min(height, width) * scaleFactor);
    float rectHeight = rectWidth / aspectRatio;

    float offsetX = width - rectWidth;
    float offsetY = height - rectHeight;

    float left = offsetX / 2;
    float top = offsetY / 2;
    float right = left + rectWidth;
    float bottom = top + rectHeight;

    return new RectF(left, top, right, bottom);
  }

  /**
   * Takes the aspect ratio string and returns it as a fraction.
   *
   * @param aspectRatioString the string containing an aspect ratio like 1:2
   * @return The calculated aspect ratio, or 1 in case of error
   */
  public static float getAspectRatioFromString(String aspectRatioString) {
    String separator = ":";
    if (aspectRatioString.contains(separator)) {
      String[] parts = aspectRatioString.split(separator);
      if (parts.length == 2) {
        try {
          float left = Integer.parseInt(parts[0]);
          float right = Integer.parseInt(parts[1]);
          return (left / right);
        } catch (NumberFormatException e) {
          // do nothing
        }
      }
    }
    return 1;
  }

  private Utils() {
    throw new IllegalStateException("Utility class");
  }

}
