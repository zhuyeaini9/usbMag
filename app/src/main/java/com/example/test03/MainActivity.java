package com.example.test03;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Set;

import Model.ADC;
import Model.HandleDataFragment;
import Model.PG;
import Model.USBMAGAXI;

public class MainActivity extends AppCompatActivity
        implements MainFragment.OnFragmentInteractionListener, CalFragment.OnFragmentInteractionListener, TerminalFragment.OnFragmentInteractionListener, SetupFragment.OnFragmentInteractionListener
        , NumPlotFragment.OnFragmentInteractionListener
{
    private MyUsbService mUsbService;
    private MyHandler mHandler;

    private BottomNavigationView mBottomNav;
    private HandleDataFragment mCurFragment = null;
    private TextView mUsbStatusMsg;

    private CheckBox mCnCheckBox;

    private CConfigSqlite mSqlite;

    private String mCurCmd = "";
    public USBMAGAXI mUSBAxis = USBMAGAXI.NONE;
    public ADC mAdc = ADC.NONE;
    public PG mPg = PG.NONE;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = item ->
    {
        switch (item.getItemId())
        {
            case R.id.navigation_home:
                mCurFragment = MainFragment.newInstance("", "");
                break;
            case R.id.navigation_calToField:
                mCurFragment = CalFragment.newInstance("", "");
                break;
            case R.id.navigation_terminal:
                mCurFragment = TerminalFragment.newInstance("", "");
                break;
            case R.id.navigation_setup:
                mCurFragment = SetupFragment.newInstance("", "");
                break;
        }
        if (mCurFragment != null)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mCurFragment).commit();
        }
        return true;
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            switch (intent.getAction())
            {
                case MyUsbService.ACTION_USB_PERMISSION_GRANTED:
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    setUsbStatus(true);
                    break;
                case MyUsbService.ACTION_USB_PERMISSION_NOT_GRANTED:
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    setUsbStatus(false);
                    break;
                case MyUsbService.ACTION_NO_USB:
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    setUsbStatus(false);
                    break;
                case MyUsbService.ACTION_USB_DISCONNECTED:
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    setUsbStatus(false);
                    break;
                case MyUsbService.ACTION_USB_NOT_SUPPORTED:
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    setUsbStatus(false);
                    break;
            }
        }
    };

    public void setCurFragment(HandleDataFragment f)
    {
        mCurFragment = f;
    }

    public void gotoPlotNumWin()
    {
        mCurFragment = NumPlotFragment.newInstance("", "");
        if (mCurFragment != null)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mCurFragment).commit();
        }
    }

    private void setUsbStatus(boolean s)
    {
        if (s)
        {
            mUsbStatusMsg.setText(getString(R.string.usb_connected));
            mUsbStatusMsg.setTextColor(Color.GREEN);
        }
        else
        {
            mUsbStatusMsg.setText(getString(R.string.usb_dis));
            mUsbStatusMsg.setTextColor(Color.RED);

            resetDevice();
        }
    }

    private void resetDevice()
    {
        mCurCmd = "";
        mUSBAxis = USBMAGAXI.NONE;
        mAdc = ADC.NONE;
        mPg = PG.NONE;
    }

    private void setFilters()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyUsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(MyUsbService.ACTION_NO_USB);
        filter.addAction(MyUsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(MyUsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(MyUsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
    {
        if (!MyUsbService.SERVICE_CONNECTED)
        {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty())
            {
                Set<String> keys = extras.keySet();
                for (String key : keys)
                {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mUsbServiceCon = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mUsbService = ((MyUsbService.UsbBinder) service).getService();
            mUsbService.setHandler(mHandler);

            if (mUsbService.isUsbConnected())
            {
                setUsbStatus(true);
                mBottomNav.setSelectedItemId(mBottomNav.getSelectedItemId());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mUsbService = null;
        }
    };

    @Override
    protected void attachBaseContext(Context newBase)
    {
        mSqlite = new CConfigSqlite(newBase);
        int l = mSqlite.getLocal();

        Resources res = newBase.getResources();
        Configuration c = res.getConfiguration();

        if (l == 0)
        {
            c.setLocale(Locale.ENGLISH);
        }
        if (l == -1)
        {
            mSqlite.updateLocal(0);
            c.setLocale(Locale.ENGLISH);
        }
        else if (l == 1)
        {
            c.setLocale(Locale.CHINA);
        }

        super.attachBaseContext(newBase.createConfigurationContext(c));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCnCheckBox = findViewById(R.id.cn_checkbox);
        if (mSqlite.getLocal() == 1)
        {
            mCnCheckBox.setChecked(true);
        }
        mCnCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            mSqlite.updateLocal(isChecked ? 1 : 0);
            recreate();
        });
        mUsbStatusMsg = findViewById(R.id.usbStatus);
        mBottomNav = findViewById(R.id.navigation);
        mBottomNav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mHandler = new MyHandler(this);

        mCurFragment = MainFragment.newInstance("", "");
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mCurFragment).commit();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Start listening notifications from UsbService
        setFilters();
        // Start UsbService(if it was not started before) and Bind it
        startService(MyUsbService.class, mUsbServiceCon, null);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(mUsbServiceCon);
        if (mUsbService != null)
        {
            mUsbService.setHandler(null);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment)
    {
    }

    private void handleData(String data)
    {
        try
        {
            if (mCurFragment != null)
            {
                mCurFragment.onHandleData(data, mCurCmd);
            }
        }
        catch (Exception e)
        {
            CHelp.showMsg(getApplicationContext(), e.toString());
        }
    }

    public void sendCmd(String cmd, int mill)
    {
        if (mUsbService != null)
        {
            if (mill > 0)
            {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(CHelp.SEND_CMD, 1, 0, cmd), mill);
            }
            else
            {
                mCurCmd = cmd;
                mUsbService.write(cmd.getBytes());
                CHelp.showMsg(getApplicationContext(), cmd);
            }
        }
    }

    public void sendCmd_Nc(String cmd, int mill)
    {
        if (mUsbService != null)
        {
            if (mill > 0)
            {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(CHelp.SEND_CMD, 0, 0, cmd), mill);
            }
            else
            {
                mUsbService.write(cmd.getBytes());
            }
        }
    }

    @Override
    public void onTerminalFragmentInteraction(Uri uri)
    {

    }

    @Override
    public void onSetupFragmentInteraction(Uri uri)
    {

    }

    @Override
    public void onCalFragmentInteraction(Uri uri)
    {

    }

    @Override
    public void onNumPlotFragmentInteraction(Uri uri)
    {

    }

    @Override
    public void onMainFragmentInteraction(int what, Object obj)
    {

    }

    private static class MyHandler extends Handler
    {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MyUsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;

                    mActivity.get().handleData(data);

                    break;
                case MyUsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case MyUsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case CHelp.SEND_CMD:
                    int changeCmd = msg.arg1;
                    if (changeCmd == 1)
                    {
                        mActivity.get().sendCmd((String) msg.obj, 0);
                    }
                    if (changeCmd == 0)
                    {
                        mActivity.get().sendCmd_Nc((String) msg.obj, 0);
                    }
                    break;
            }
        }

    }
}
