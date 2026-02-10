package com.common.utils;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;


import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;

public class AppSignatureManager {
    private final String TAG = "TAG----->signature";

    private HashMap<Type, ArrayList<String>> mSignMap = new HashMap<>();

    private enum Type {
        MD5("MD5"), SHA1("SHA1"), SHA256("SHA256");
        final String value;

        Type(String value) {
            this.value = value;
        }
    }


    private AppSignatureManager() {
    }

    private static class InnerClass {
        private static final AppSignatureManager INSTANCE = new AppSignatureManager();
    }

    public static AppSignatureManager getInstance() {
        return InnerClass.INSTANCE;
    }

    public String getMD5(Context mContext) {
        String res = "";
        ArrayList<String> mList = getSignInfo(mContext, Type.MD5);
        if (mList != null && mList.size() != 0) {
            res = mList.get(0);
        }
        return res;
    }

    public String getSHA1(Context mContext) {
        String res = "";
        ArrayList<String> mList = getSignInfo(mContext, Type.SHA1);
        if (mList != null && mList.size() != 0) {
            res = mList.get(0);
        }
        return res;
    }

    public String getSHA256(Context mContext) {
        String res = "";
        ArrayList<String> mList = getSignInfo(mContext, Type.SHA256);
        if (mList != null && mList.size() != 0) {
            res = mList.get(0);
        }
        return res;
    }

    private ArrayList<String> getSignInfo(Context context, Type type) {
        if (context == null || type == null) {
            return null;
        }
        String packageName = context.getPackageName();
        if (packageName == null) {
            return null;
        }
        if (mSignMap.get(type) != null) {
            return mSignMap.get(type);
        }
        ArrayList<String> mList = new ArrayList<String>();
        try {
            Signature[] signs = getSignatures(context, packageName);
            if (signs != null) {
                for (Signature sig : signs) {
                    String tmp = "error!";
                    switch (type) {
                        case MD5:
                            tmp = getSignatureByteString(sig, Type.MD5);
                            break;
                        case SHA1:
                            tmp = getSignatureByteString(sig, Type.SHA1);
                            break;
                        case SHA256:
                            tmp = getSignatureByteString(sig, Type.SHA256);
                            break;
                    }
                    mList.add(tmp);
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
        }
        mSignMap.put(type, mList);
        return mList;
    }

    private Signature[] getSignatures(Context context, String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return packageInfo.signatures;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getSignatureByteString(Signature sig, Type type) {
        byte[] hexBytes = sig.toByteArray();
        String fingerprint = "error!";
        try {
            MessageDigest digest = MessageDigest.getInstance(type.value);
            byte[] digestBytes = digest.digest(hexBytes);
            StringBuilder sb = new StringBuilder();
            for (byte digestByte : digestBytes) {
                sb.append(((Integer.toHexString((digestByte & 0xFF) | 0x100)).substring(1, 3)).toUpperCase());
                sb.append(":");
            }
            fingerprint = sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            LogUtil.e(TAG, "getSignatureByteString failed", e.getMessage());
        }

        return fingerprint;
    }

}
