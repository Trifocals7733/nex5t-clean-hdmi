package com.sony.scalar.hardware;

// Compile-only stub; the real class is in the camera's /system/framework.
public class CameraEx {
    public static class OpenOptions {}
    public class ParametersModifier {
        public int getISOSensitivity() { throw new RuntimeException("stub"); }
        public void setISOSensitivity(int iso) { throw new RuntimeException("stub"); }
        public int getAperture() { throw new RuntimeException("stub"); }
        public android.util.Pair<Integer, Integer> getShutterSpeed() { throw new RuntimeException("stub"); }
        public java.util.List<Integer> getSupportedISOSensitivities() { throw new RuntimeException("stub"); }
    }
    public static CameraEx open(int id, OpenOptions options) { throw new RuntimeException("stub"); }
    public android.hardware.Camera getNormalCamera() { throw new RuntimeException("stub"); }
    public android.hardware.Camera.Parameters createEmptyParameters() { throw new RuntimeException("stub"); }
    public ParametersModifier createParametersModifier(android.hardware.Camera.Parameters p) { throw new RuntimeException("stub"); }
    public void incrementAperture() { throw new RuntimeException("stub"); }
    public void decrementAperture() { throw new RuntimeException("stub"); }
    public void incrementShutterSpeed() { throw new RuntimeException("stub"); }
    public void decrementShutterSpeed() { throw new RuntimeException("stub"); }
    public void release() { throw new RuntimeException("stub"); }
}
