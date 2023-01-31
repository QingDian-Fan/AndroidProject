package com.dian.demo.repository.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


/**
 * 样例
 * @Database(entities = {DatabaseEntity.class}, version = 1, exportSchema = false)
 */
public abstract class DatabaseFactory extends RoomDatabase {

    private static volatile DatabaseFactory dataBase;

    public static DatabaseFactory getInstance(Context mContext) {
        if (dataBase == null) {
            synchronized (DatabaseFactory.class){
                if (dataBase==null){
                    dataBase = Room.databaseBuilder(mContext.getApplicationContext(), DatabaseFactory.class, "db_database_entity")
                            .build();
                }
            }
        }
        return dataBase;
    }

    /**
     *  样例
     *  public abstract DatabaseDao getDatabase();
     */



}
