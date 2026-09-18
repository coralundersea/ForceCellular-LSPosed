package de.robv.android.xposed;

public class XposedBridge {
    public static void log(String text) {}
    public static void log(Throwable t) {}

    public static void hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook hook) {
        // stub for compile time
    }
}
