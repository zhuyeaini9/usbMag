package com.example.test03;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

public class CConfigSqlite extends SQLiteOpenHelper
{
    private final static String mDbName = "Config.db";
    private final static String mTbName = "USBMagConfig";
    private final static int mVersion = 1;

    public CConfigSqlite(@Nullable Context context)
    {
        super(context, mDbName, null, mVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String sql = String.format("create table if not exists %s (config_name varchar(64),config_value varchar(256))",mTbName);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        String sql = String.format("drop table %s",mTbName);
        db.execSQL(sql);
    }

    public int getLocal()
    {
        int re = -1;
        String sql = String.format("select config_value from %s where config_name='Local'",mTbName);
        Cursor c = getReadableDatabase().rawQuery(sql,null);
        if(c!=null)
        {
            try
            {
                if(c.getCount()>0)
                {
                    c.moveToFirst();
                    re = Integer.parseInt(c.getString(0));
                }
            }
            catch (Exception e)
            {
                Log.i("exp",e.toString());
            }
            c.close();
        }

        return re;
    }

    public void updateLocal(int local)
    {
        String sql = String.format("select config_value from %s where config_name='Local'",mTbName);
        Cursor c = getReadableDatabase().rawQuery(sql,null);
        if(c!=null)
        {
            if(c.getCount()>0)
            {
                //update
                c.close();
                sql = String.format("update %s set config_value = '%s' where config_name='Local'",mTbName,Integer.toString(local));
                getWritableDatabase().execSQL(sql);
            }
            else
            {
                //insert
                c.close();
                sql = String.format("insert into %s values ('Local','%s')",mTbName,Integer.toString(local));
                getWritableDatabase().execSQL(sql);
            }
        }
    }
}
