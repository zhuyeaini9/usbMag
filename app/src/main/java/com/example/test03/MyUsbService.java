package com.example.test03;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MyUsbService extends Service
{
    public static final String TAG = "UsbService";

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;

    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 115200; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    private IBinder mBinder = new UsbBinder();
    private Context mContext;
    private Handler mHandler;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbSerialDevice mSerialPort;

    private boolean mSerialPortConnected;

    public MyUsbService()
    {
    }

    public boolean isUsbConnected()
    {
        return mSerialPortConnected;
    }

    public class UsbBinder extends Binder
    {
        public MyUsbService getService()
        {
            return MyUsbService.this;
        }
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback()
    {
        @Override
        public void onReceivedData(byte[] arg0)
        {
            try
            {
                String data = new String(arg0, "UTF-8");
                if (mHandler != null)
                {
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
                }
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION))
            {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted)
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    mConnection = mUsbManager.openDevice(mDevice);
                    new ConnectionThread().start();
                }
                else
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            }
            else if (arg1.getAction().equals(ACTION_USB_ATTACHED))
            {
                if (!mSerialPortConnected)
                {
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
                }
            }
            else if (arg1.getAction().equals(ACTION_USB_DETACHED))
            {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (mSerialPortConnected)
                {
                    mSerialPort.close();
                }
                mSerialPortConnected = false;
            }
        }
    };

    private void findSerialPortDevice()
    {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty())
        {

            // first, dump the hashmap for diagnostic purposes
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                mDevice = entry.getValue();
                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                        mDevice.getVendorId(), mDevice.getProductId(),
                        UsbSerialDevice.isSupported(mDevice),
                        mDevice.getDeviceClass(), mDevice.getDeviceSubclass(),
                        mDevice.getDeviceName()));
            }

            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                mDevice = entry.getValue();

                if (UsbSerialDevice.isSupported(mDevice))
                {
                    requestUserPermission();
                    break;
                }
                else
                {
                    mConnection = null;
                    mDevice = null;
                }
            }
            if (mDevice == null)
            {
                // There are no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        }
        else
        {
            Log.d(TAG, "findSerialPortDevice() usbManager returned empty device list.");
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void requestUserPermission()
    {
        Log.d(TAG, String.format("requestUserPermission(%X:%X)", mDevice.getVendorId(), mDevice.getProductId()));
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(mDevice, mPendingIntent);
    }

    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback()
    {
        @Override
        public void onCTSChanged(boolean state)
        {
            if (mHandler != null)
            {
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
            }
        }
    };

    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback()
    {
        @Override
        public void onDSRChanged(boolean state)
        {
            if (mHandler != null)
            {
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
            }
        }
    };

    private class ConnectionThread extends Thread
    {
        @Override
        public void run()
        {
            mSerialPort = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection);
            if (mSerialPort != null)
            {
                if (mSerialPort.open())
                {
                    mSerialPortConnected = true;
                    mSerialPort.setBaudRate(BAUD_RATE);
                    mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    mSerialPort.read(mCallback);
                    mSerialPort.getCTS(ctsCallback);
                    mSerialPort.getDSR(dsrCallback);

                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going 
                    // to be uploaded or not
                    // Thread.sleep(2000); 
                    // sleep some. YMMV with different chips.

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    mContext.sendBroadcast(intent);
                }
                else
                {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (mSerialPort instanceof CDCSerialDevice)
                    {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    }
                    else
                    {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    }
                }
            }
            else
            {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                mContext.sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onCreate()
    {
        this.mContext = this;
        mSerialPortConnected = false;
        MyUsbService.SERVICE_CONNECTED = true;
        setFilter();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    private void setFilter()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mSerialPort.close();
        unregisterReceiver(mUsbReceiver);
        MyUsbService.SERVICE_CONNECTED = false;
    }

    public void write(byte[] data)
    {
        if (mSerialPort != null)
        {
            mSerialPort.write(data);
        }
    }

    public void setHandler(Handler mHandler)
    {
        this.mHandler = mHandler;
    }

}
