package com.dian.demo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dian.demo.R;
import com.dian.demo.ui.activity.GenerateActivity;
import com.dian.demo.ui.activity.ScanActivity;

import java.util.ArrayList;
import java.util.List;

//通过指定的id disable一个shortcut，底层逻辑跟removeDynamicShortcuts一致，快捷方式会被移除
// shortcutManager.disableShortcuts(Collections.singletonList(SHORTCUT_ID_LIKE),"disable shortcut");

public class ShortCutUtil {

    private static final String SHORTCUT_ID_SCAN = "shortcut_id_scan";
    private static final String SHORTCUT_ID_GENERATE = "shortcut_id_generate";

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    public List<ShortcutInfo> createShortCut(Context mContext) {
        List<ShortcutInfo> shortList = new ArrayList<>();
        ShortcutInfo scanShortCut = new ShortcutInfo.Builder(mContext, SHORTCUT_ID_SCAN)
                .setShortLabel(ResourcesUtil.getString((R.string.short_cut_scan)))
                .setLongLabel(ResourcesUtil.getString(R.string.short_cut_scan))
                .setIcon(Icon.createWithResource(mContext, R.mipmap.icon_short_cut_scan))
                .setIntent(new Intent(Intent.ACTION_VIEW)
                .setClass(mContext, ScanActivity.class))//intent必须设置action
                .build();
        ShortcutInfo generateShortCut = new ShortcutInfo.Builder(mContext, SHORTCUT_ID_GENERATE)
                .setShortLabel(ResourcesUtil.getString((R.string.short_cut_generate)))
                .setLongLabel(ResourcesUtil.getString(R.string.short_cut_generate))
                .setIcon(Icon.createWithResource(mContext,  R.mipmap.icon_short_cut_genrate))
                .setIntent(new Intent(Intent.ACTION_VIEW)
                .setClass(mContext, GenerateActivity.class))//intent必须设置action
                .build();
        shortList.add(scanShortCut);
        shortList.add(generateShortCut);
        return shortList;
    }
}
