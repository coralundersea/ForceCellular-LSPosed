package com.force.cellular;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Enumeration;
import java.util.WeakHashMap;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "ForceCellular";
    // Hardcoded Cellular only mode
    private static final boolean SPOOF_WIFI = false;
    private static final boolean SPOOF_CELLULAR = true;

    // Cache for NetworkInterface name overrides (same technique as original)
    private static final WeakHashMap<Object, String> nameOverrides = new WeakHashMap<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log(TAG + ": Loading hooks for " + lpparam.packageName);

        try {
            // ========== 1. NetworkCapabilities.hasTransport ==========
            Class<?> NetworkCapabilities = XposedHelpers.findClass(
                    "android.net.NetworkCapabilities", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(NetworkCapabilities, "hasTransport", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int transport = (Integer) param.args[0];
                    // 0 = CELLULAR, 1 = WIFI, 2 = BLUETOOTH, 3 = ETHERNET, 4 = VPN
                    if (transport == 1) { // WIFI
                        param.setResult(SPOOF_WIFI);
                    } else if (transport == 0) { // CELLULAR
                        param.setResult(SPOOF_CELLULAR);
                    } else if (transport == 3) { // ETHERNET
                        param.setResult(false);
                    }
                }
            });

            // ========== 2. NetworkInfo ==========
            Class<?> NetworkInfo = XposedHelpers.findClass("android.net.NetworkInfo", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(NetworkInfo, "getType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // TYPE_MOBILE = 0, TYPE_WIFI = 1
                    param.setResult(0); // always MOBILE
                }
            });

            XposedHelpers.findAndHookMethod(NetworkInfo, "getTypeName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult("MOBILE");
                }
            });

            XposedHelpers.findAndHookMethod(NetworkInfo, "isConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            XposedHelpers.findAndHookMethod(NetworkInfo, "isConnectedOrConnecting", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            // ========== 3. WifiManager - force WiFi disabled ==========
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
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": WifiManager hook failed: " + t);
            }

            // ========== 4. TelephonyManager - force LTE connected ==========
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
                        param.setResult(13);
                    }
                });

                XposedHelpers.findAndHookMethod(TelephonyManager, "getDataState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(2); // DATA_CONNECTED
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": TelephonyManager hook failed: " + t);
            }

            // ========== 5. NetworkInterface name spoof (important for some games) ==========
            try {
                // Hook getNetworkInterfaces to build name map
                Class<?> NetworkInterface = java.net.NetworkInterface.class;

                XposedBridge.hookAllMethods(NetworkInterface, "getNetworkInterfaces", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Enumeration<?> en = (Enumeration<?>) param.getResult();
                            if (en == null) return;
                            while (en.hasMoreElements()) {
                                Object ni = en.nextElement();
                                String realName = (String) XposedHelpers.callMethod(ni, "getName");
                                if (realName != null && (realName.contains("wlan") || realName.contains("eth") || realName.contains("wlan0"))) {
                                    nameOverrides.put(ni, "rmnet_data0");
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                });

                XposedBridge.hookAllMethods(NetworkInterface, "getName", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String override = nameOverrides.get(param.thisObject);
                        if (override != null) {
                            param.setResult(override);
                        }
                    }
                });

                XposedBridge.hookAllMethods(NetworkInterface, "getDisplayName", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String override = nameOverrides.get(param.thisObject);
                        if (override != null) {
                            param.setResult(override);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": NetworkInterface hook failed: " + t);
            }

            XposedBridge.log(TAG + ": All Cellular-only hooks applied for " + lpparam.packageName);

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Fatal error in hooks: " + t);
        }
    }
}
