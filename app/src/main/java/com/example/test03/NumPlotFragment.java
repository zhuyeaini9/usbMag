package com.example.test03;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import Model.ADC;
import Model.CVector3;
import Model.HandleDataFragment;
import Model.PG;
import Model.USBMAGAXI;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NumPlotFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NumPlotFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NumPlotFragment extends HandleDataFragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String mRevData = "";
    public USBMAGAXI mUSBAxis = USBMAGAXI.NONE;

    private OnFragmentInteractionListener mListener;

    private GraphView mPlot;
    private LineGraphSeries<DataPoint> mSeriesX = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mSeriesY = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mSeriesZ = new LineGraphSeries<>();
    private Button mPauseResumeBut;
    private boolean mRunStatus = true;
    private Spinner mAxiSpinner;
    private int mCurSelAx = 0;
    private TextView mNumTV;

    public NumPlotFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NumPlotFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NumPlotFragment newInstance(String param1, String param2)
    {
        NumPlotFragment fragment = new NumPlotFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_num_plot, container, false);

        mNumTV = v.findViewById(R.id.numTV);
        mAxiSpinner = v.findViewById(R.id.axiSP);
        mAxiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                mCurSelAx = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
        mPauseResumeBut = v.findViewById(R.id.pauseResumeBut);
        mPauseResumeBut.setOnClickListener(vi ->
        {
            if (mRunStatus)
            {
                sendCmd_Nc("PE", 0);
            }
            else
            {
                sendCmd_Nc("RE", 0);
            }
            mRunStatus = !mRunStatus;
            setRunStatusButText();
        });
        setRunStatusButText();

        mPlot = v.findViewById(R.id.plot);

        mSeriesX.setColor(Color.RED);
        mSeriesX.setTitle("X");
        mSeriesY.setColor(Color.BLUE);
        mSeriesY.setTitle("Y");
        mSeriesZ.setColor(Color.GREEN);
        mSeriesZ.setTitle("Z");

        mPlot.getGridLabelRenderer().setHorizontalAxisTitle("Time(s)");
        mPlot.getGridLabelRenderer().setVerticalAxisTitle("Oe");
        mPlot.getGridLabelRenderer().setLabelVerticalWidth(100);

        mPlot.getViewport().setXAxisBoundsManual(true);
        mPlot.getViewport().setMinX(0);
        mPlot.getViewport().setMaxX(10);

        mPlot.getViewport().setScrollable(true);
        mPlot.getViewport().setScrollableY(true);
        mPlot.getViewport().setScalable(true);
        mPlot.getViewport().setScalableY(true);

        mPlot.addSeries(mSeriesX);

        mPlot.getLegendRenderer().setVisible(true);
        mPlot.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        return v;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
            ((MainActivity) mListener).setCurFragment(this);

            initCmd();
        }
        else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onHandleData(String data, String cmd)
    {
        if (cmd == "RM")
        {
            mRevData += data;
            int i = mRevData.indexOf("RD");
            if (i == -1)
            {
                return;
            }
            mRevData = mRevData.substring(i);
            i = 0;
            int j = mRevData.indexOf("\r\n");
            if (j == -1)
            {
                return;
            }

            String tarStr = mRevData.substring(i, j);
            tarStr.trim();

            if (mRevData.length() == j + 2)
            {
                mRevData = "";
            }
            else
            {
                mRevData = mRevData.substring(j + 2);
            }

            String[] parts = tarStr.split(",");
            setAxi(parts.length);
        }
        if (cmd == "RC")
        {
            if (USBMAGAXI.NONE == mUSBAxis)
            {
                return;
            }

            mRevData += data;
            int i = mRevData.indexOf("RD");
            if (i == -1)
            {
                return;
            }
            mRevData = mRevData.substring(i);
            i = mRevData.indexOf("RD");

            int j = mRevData.indexOf("\r\n");
            if (j == -1)
            {
                return;
            }

            String tarStr = mRevData.substring(i, j);
            tarStr.trim();

            if (mRevData.length() == j + 2)
            {
                mRevData = "";
            }
            else
            {
                mRevData = mRevData.substring(j + 2);
            }

            CVector3 curData = parseCmd_RC(tarStr);
            if (curData == null)
            {
                return;
            }

            if (mUSBAxis == USBMAGAXI.ONE_AXIS)
            {
                mNumTV.setText(Float.toString(curData.mX));
                mSeriesX.appendData(new DataPoint(curData.mT, curData.mX), true, 1500);
            }
            if (mUSBAxis == USBMAGAXI.THREE_AXIS)
            {
                showAxiText(curData);
                mSeriesX.appendData(new DataPoint(curData.mT, curData.mX), true, 1500);
                mSeriesY.appendData(new DataPoint(curData.mT, curData.mY), true, 1500);
                mSeriesZ.appendData(new DataPoint(curData.mT, curData.mZ), true, 1500);
            }
        }
    }

    private void showAxiText(CVector3 data)
    {
        if (data == null)
        {
            return;
        }

        if (mCurSelAx == 0)
        {
            mNumTV.setText(String.format("%.05f", data.mX));
        }
        if (mCurSelAx == 1)
        {
            mNumTV.setText(String.format("%.05f", data.mY));
        }
        if (mCurSelAx == 2)
        {
            mNumTV.setText(String.format("%.05f", data.mZ));
        }
        if (mCurSelAx == 3)
        {
            mNumTV.setText(String.format("%.05f", data.getMag()));
        }
    }

    private CVector3 parseCmd_RC(String data)
    {
        try
        {
            CVector3 curData = new CVector3();

            int rdIndex = data.indexOf("RD");
            if (rdIndex == -1)
            {
                return null;
            }

            data = data.substring(rdIndex);

            String[] parts = data.split(",");
            if (parts.length == 4)
            {
                curData.mX = Float.parseFloat(parts[1]);
                curData.mY = Float.parseFloat(parts[2]);
                curData.mZ = Float.parseFloat(parts[3]);

                String xV = parts[0];
                String fV = "RD";
                xV = xV.substring(xV.lastIndexOf(fV) + fV.length());
                curData.mT = Float.parseFloat(xV);

                return curData;
            }
            if (parts.length == 2)
            {
                curData.mX = Float.parseFloat(parts[1]);

                String xV = parts[0];
                String fV = "RD";
                xV = xV.substring(xV.lastIndexOf(fV) + fV.length());
                curData.mT = Float.parseFloat(xV);

                return curData;
            }
        }
        catch (Exception e)
        {
            Log.i("exp", e.toString());
        }

        return null;
    }

    private void setRunStatusButText()
    {
        if (mRunStatus)
        {
            mPauseResumeBut.setText(R.string.pause);
        }
        else
        {
            mPauseResumeBut.setText(R.string.resume);
        }
    }

    private void setAxi(int axi)
    {
        if (mListener == null)
        {
            return;
        }

        if (axi == 1)
        {
            mUSBAxis = USBMAGAXI.ONE_AXIS;
            mPlot.removeAllSeries();
            mPlot.addSeries(mSeriesX);

            ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(
                    getContext(), R.array.axis_x, R.layout.spinner_item);
            ad.setDropDownViewResource(R.layout.spinner_item);
            mAxiSpinner.setAdapter(ad);
        }
        if (axi == 3)
        {
            mUSBAxis = USBMAGAXI.THREE_AXIS;
            mPlot.removeAllSeries();
            mPlot.addSeries(mSeriesX);
            mPlot.addSeries(mSeriesY);
            mPlot.addSeries(mSeriesZ);

            ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(
                    getContext(), R.array.axis_xyz, R.layout.spinner_item);
            ad.setDropDownViewResource(R.layout.spinner_item);
            mAxiSpinner.setAdapter(ad);
        }
    }

    private void sendCmd(String cmd, int mill)
    {
        if (mListener == null)
        {
            return;
        }

        MainActivity m = (MainActivity) mListener;
        m.sendCmd(cmd, mill);
    }

    private void sendCmd_Nc(String cmd, int mill)
    {
        if (mListener == null)
        {
            return;
        }

        MainActivity m = (MainActivity) mListener;
        m.sendCmd_Nc(cmd, mill);
    }

    private void initCmd()
    {
        if (mListener == null)
        {
            return;
        }

        MainActivity m = (MainActivity) mListener;
        mRevData = "";

        sendCmd("PE", 500);
        if (m.mAdc == ADC.NONE)
        {
            m.mAdc = ADC.NORMAL;
        }
        if (m.mPg == PG.NONE)
        {
            m.mPg = PG.LOW_NOISE;
        }
        sendCmd(m.mAdc == ADC.NORMAL ? "DB 16" : "DB 12", 1000);
        sendCmd(m.mPg == PG.LOW_NOISE ? "PG 8" : "PG 1", 1500);
        sendCmd("RM", 2000);
        sendCmd("RC", 2500);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onNumPlotFragmentInteraction(Uri uri);
    }
}
