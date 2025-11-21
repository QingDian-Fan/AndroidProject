package com.dian.demo.utils.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIModeManager {
    private final List<UIModeListener> uiModeListenerList = new ArrayList<>();
    private static final UIModeManager instance = new UIModeManager();

    public static UIModeManager getInstance() {
        return instance;
    }

    public void registerUIModeListener(UIModeListener uiModeListener) {
        boolean hava = false;
        for (int i = 0; i < uiModeListenerList.size(); i++) {
            if (uiModeListenerList.get(i) == uiModeListener) {
                hava = true;
            }
        }
        if (!hava) {
            uiModeListenerList.add(uiModeListener);
        }
    }

    public void unRegisterUIModeListener(UIModeListener uiModeListener) {
        uiModeListenerList.remove(uiModeListener);
    }

    public void broadCastUiModeChanged(boolean isNight) {
        for (int i = 0; i < uiModeListenerList.size(); i++) {
            uiModeListenerList.get(i).uiModeChanged(isNight);
        }
    }

}
