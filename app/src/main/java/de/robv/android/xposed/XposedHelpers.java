package de.robv.android.xposed;

public class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... args) {
        // stub for compile time
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Throwable t) {
            return null;
        }
    }
}
