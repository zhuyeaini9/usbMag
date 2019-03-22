package com.example.test03;

import android.content.Context;
import android.widget.Toast;

public class CHelp
{
    public static final int SEND_CMD = 100;
    public static final int ASK_AXI = 101;

    public static void showMsg(Context c, String msg)
    {
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }
}
