package de.robv.android.xposed;
public class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try { return Class.forName(className, false, classLoader); }
        catch (Throwable t) { return null; }
    }
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... args) {}
}
