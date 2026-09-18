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
        XposedBridge.log(TAG + ": Hooking " + lpparam.packageName);

        try {
            // NetworkCapabilities.hasTransport
            Class<?> nc = XposedHelpers.findClass("android.net.NetworkCapabilities", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(nc, "hasTransport", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int t = (Integer) param.args[0];
                    if (t == 1) param.setResult(false);      // WIFI
                    else if (t == 0) param.setResult(true);  // CELLULAR
                    else if (t == 3) param.setResult(false); // ETHERNET
                }
            });

            // NetworkInfo
            Class<?> ni = XposedHelpers.findClass("android.net.NetworkInfo", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(ni, "getType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(0); // TYPE_MOBILE
                }
            });
            XposedHelpers.findAndHookMethod(ni, "getTypeName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult("MOBILE");
                }
            });
            XposedHelpers.findAndHookMethod(ni, "isConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            // WifiManager
            try {
                Class<?> wm = XposedHelpers.findClass("android.net.wifi.WifiManager", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(wm, "isWifiEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
                XposedHelpers.findAndHookMethod(wm, "getWifiState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(1); // DISABLED
                    }
                });
            } catch (Throwable ignored) {}

            // TelephonyManager
            try {
                Class<?> tm = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(tm, "getNetworkType", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(13); // LTE
                    }
                });
                XposedHelpers.findAndHookMethod(tm, "getDataNetworkType", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(13);
                    }
                });
                XposedHelpers.findAndHookMethod(tm, "getDataState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(2); // CONNECTED
                    }
                });
            } catch (Throwable ignored) {}

            // NetworkInterface name
            try {
                Class<?> nif = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(nif, "getName", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String n = (String) param.getResult();
                        if (n != null && (n.contains("wlan") || n.contains("eth"))) {
                            param.setResult("rmnet_data0");
                        }
                    }
                });
            } catch (Throwable ignored) {}

            XposedBridge.log(TAG + ": Hooks applied for " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error " + t);
        }
    }
}
