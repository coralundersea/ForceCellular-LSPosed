package com.force.cellular;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "ForceCellular";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 如果你只想对特定游戏生效，取消下面注释并改成真实包名
        // if (!lpparam.packageName.equals("com.example.game")) return;

        XposedBridge.log(TAG + ": Hooking package → " + lpparam.packageName);

        try {
            // ========== 1. NetworkCapabilities.hasTransport ==========
            Class<?> NetworkCapabilities = XposedHelpers.findClass(
                    "android.net.NetworkCapabilities", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(NetworkCapabilities, "hasTransport", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int transport = (int) param.args[0];
                    // 0 = TRANSPORT_CELLULAR
                    // 1 = TRANSPORT_WIFI
                    // 3 = TRANSPORT_ETHERNET
                    if (transport == 1) { // WIFI → 强制 false
                        param.setResult(false);
                    } else if (transport == 0) { // CELLULAR → 强制 true
                        param.setResult(true);
                    } else if (transport == 3) { // ETHERNET → 也关掉
                        param.setResult(false);
                    }
                }
            });

            // ========== 2. NetworkInfo 关键 ==========
            Class<?> NetworkInfo = XposedHelpers.findClass("android.net.NetworkInfo", lpparam.classLoader);

            // getType() → TYPE_MOBILE = 0
            XposedHelpers.findAndHookMethod(NetworkInfo, "getType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(0); // TYPE_MOBILE
                }
            });

            // getTypeName()
            XposedHelpers.findAndHookMethod(NetworkInfo, "getTypeName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult("MOBILE");
                }
            });

            // isConnected
            XposedHelpers.findAndHookMethod(NetworkInfo, "isConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            // isConnectedOrConnecting
            XposedHelpers.findAndHookMethod(NetworkInfo, "isConnectedOrConnecting", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            // ========== 3. WifiManager 强制 WiFi 关闭 ==========
            try {
                Class<?> WifiManager = XposedHelpers.findClass(
                        "android.net.wifi.WifiManager", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(WifiManager, "isWifiEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });

                XposedHelpers.findAndHookMethod(WifiManager, "getWifiState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(1); // WIFI_STATE_DISABLED
                    }
                });
            } catch (Throwable ignored) {}

            // ========== 4. TelephonyManager 伪装成 LTE 流量 ==========
            try {
                Class<?> TelephonyManager = XposedHelpers.findClass(
                        "android.telephony.TelephonyManager", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(TelephonyManager, "getNetworkType", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(13); // NETWORK_TYPE_LTE
                    }
                });

                XposedHelpers.findAndHookMethod(TelephonyManager, "getDataNetworkType", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(13); // LTE
                    }
                });

                XposedHelpers.findAndHookMethod(TelephonyManager, "getDataState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(2); // DATA_CONNECTED
                    }
                });
            } catch (Throwable ignored) {}

            // ========== 5. NetworkInterface 名字伪装（增强绕过） ==========
            try {
                Class<?> NetworkInterface = XposedHelpers.findClass(
                        "java.net.NetworkInterface", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(NetworkInterface, "getName", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String name = (String) param.getResult();
                        if (name != null && (name.toLowerCase().contains("wlan") || name.toLowerCase().contains("eth"))) {
                            param.setResult("rmnet_data0");
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(NetworkInterface, "getDisplayName", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String name = (String) param.getResult();
                        if (name != null && (name.toLowerCase().contains("wlan") || name.toLowerCase().contains("eth"))) {
                            param.setResult("rmnet_data0");
                        }
                    }
                });
            } catch (Throwable ignored) {}

            XposedBridge.log(TAG + ": All force-cellular hooks applied successfully for " + lpparam.packageName);

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook failed → " + t);
        }
    }
}
