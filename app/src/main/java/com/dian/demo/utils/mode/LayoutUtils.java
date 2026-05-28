package com.dian.demo.utils.mode;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.project.common.utils.LogUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class LayoutUtils {
    private static final String TAG = LayoutUtils.class.getSimpleName() + "-------->";

    public static void updateResUI(Context context, @LayoutRes int layoutId, View rootView) {
        XmlResourceParser xmlParser = context.getResources().getLayout(layoutId);
        try {
            int event = xmlParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        LogUtil.e(TAG, "xml解析开始");
                        break;
                    case XmlPullParser.START_TAG:
                        String id = xmlParser.getAttributeValue("http://schemas.android.com/apk/res/android", "id");
                        if (checkAttr(id)) {
                            id = id.replace("@", "");
                            View view = rootView.findViewById(Integer.parseInt(id));
                            String background = xmlParser.getAttributeValue("http://schemas.android.com/apk/res/android", "background");
                            String src = xmlParser.getAttributeValue("http://schemas.android.com/apk/res/android", "src");
                            String textColor = xmlParser.getAttributeValue("http://schemas.android.com/apk/res/android", "textColor");
                            if (checkAttr(background)) {
                                background = background.replace("@", "");
                                view.setBackground(context.getResources().getDrawable(Integer.parseInt(background)));
                                LogUtil.e(TAG, "update background");
                            }
                            if (view instanceof ImageView && checkAttr(src)) {
                                src = src.replace("@", "");
                                ((ImageView) view).setImageDrawable(context.getResources().getDrawable(Integer.parseInt(src)));
                                LogUtil.e(TAG, "update src");
                            }
                            if (view instanceof TextView && checkAttr(textColor)) {
                                textColor = textColor.replace("@", "");
                                ((TextView) view).setTextColor(context.getResources().getColor(Integer.parseInt(textColor)));
                                LogUtil.e(TAG, "update textColor");
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        LogUtil.d(TAG, "Text:" + xmlParser.getText());
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkAttr(String attr) {
        if (!TextUtils.isEmpty(attr) && !attr.equals("null") && attr.contains("@")) {
            return true;
        }
        return false;
    }
}
