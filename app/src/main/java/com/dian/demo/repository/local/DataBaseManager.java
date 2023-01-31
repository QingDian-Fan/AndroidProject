package com.dian.demo.repository.local;

import android.content.Context;


public class DataBaseManager {


    private static volatile DataBaseManager mInstance;

    private Context mContext;


    private DataBaseManager(Context mContext) {
        this.mContext = mContext;
    }

    public static DataBaseManager getInstance(Context mContext) {
        if (mInstance == null) {
            synchronized (DataBaseManager.class) {
                if (mInstance == null) {
                    mInstance = new DataBaseManager(mContext);
                }
            }
        }
        return mInstance;
    }

    /**
    *  样例
    *  public void deleteAllDatabaseEntities() {
    *         workService.execute(() -> {
    *             DatabaseFactory.getInstance(mContext).getDatabase().deleteAllDatabaseEntities();
    *         });
    *     }
    */


}
