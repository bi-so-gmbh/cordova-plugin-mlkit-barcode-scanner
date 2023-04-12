package com.biso.cordova.plugins.mlkit.barcode.scanner;

import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;

public class Utils {

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

  /**
   * Takes a rectangle and returns a copy rotated by rotationAngle
   *
   * @param rectF         the rectangle to rotate
   * @param rotationAngle the angle by which to rotate
   * @return A RectF containing the rotated rectangle
   */
  public static RectF rotateRectF(RectF rectF, float rotationAngle) {
    RectF rotated = new RectF(rectF);
    Matrix matrix = new Matrix();
    matrix.setRotate(rotationAngle, rectF.centerX(), rectF.centerY());
    matrix.mapRect(rotated);

    return rotated;
  }

  /**
   * Generates a translation matrix that translates coordinates in the source rectangle to
   * coordinates in the destination rectangle
   *
   * @param source      A RectF to use as a source
   * @param destination The target RectF
   * @return a translation matrix
   */
  public static Matrix getTranslationMatrix(RectF source, RectF destination) {
    Matrix matrix = new Matrix();
    matrix.setRectToRect(source, destination, ScaleToFit.FILL);
    return matrix;
  }

  /**
   * Wrapper to map a Rect object inplace since Matrix only does RectF
   *
   * @param rect   rect to map
   * @param matrix matrix that does the mapping
   */
  public static void mapRectInplace(Rect rect, Matrix matrix) {
    RectF rectF = new RectF(rect);
    matrix.mapRect(rectF);
    rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
  }

  /**
   * Wrapper to map a Rect object and return a mapped RectF
   *
   * @param rect   rect to map
   * @param matrix matrix that does the mapping
   * @return a Pair with the RectF as 1st parameter and a Boolean with orientation information as 2nd
   */
  public static Pair<RectF, Boolean> mapRect(Rect rect, Matrix matrix) {
    RectF rectF = new RectF(rect);
    matrix.mapRect(rectF);
    return new Pair<>(rectF, rect.height() > rect.width());
  }

  private Utils() {
    throw new IllegalStateException("Utility class");
  }

}
