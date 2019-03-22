package com.example.test03;

import java.util.ArrayList;

import Model.CCmd;

public class CRunCmdTask
{
    public CRunCmdTask(int id,String... cmds)
    {
        mId = id;
        for(String s : cmds)
        {
            mCmds.add(new CCmd(s));
        }
    }
    protected int mId;
    protected ArrayList<CCmd> mCmds = new ArrayList<CCmd>();

    protected boolean doTask()
    {
        return false;
    }
}
