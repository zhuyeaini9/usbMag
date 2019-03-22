package com.example.test03;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import Model.ADC;
import Model.HandleDataFragment;
import Model.PG;
import Model.USBMAGAXI;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends HandleDataFragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private String mParseCmd="";
    private Button mStartBut;

    public MainFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2)
    {
        MainFragment fragment = new MainFragment();
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
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        RadioGroup r = v.findViewById(R.id.pgRadioGroup);
        r.setOnCheckedChangeListener((g, id) ->
        {
            if (mListener != null)
            {
                MainActivity m = (MainActivity) mListener;
                if (id == R.id.dynamicRadio)
                {
                    m.mPg = PG.DYNAMIC;
                }
                if (id == R.id.lowNoiseRadio)
                {
                    m.mPg = PG.LOW_NOISE;
                }
            }
        });

        r = v.findViewById(R.id.adcRadioGroup);
        r.setOnCheckedChangeListener((g, id) ->
        {
            if (mListener != null)
            {
                MainActivity m = (MainActivity) mListener;
                if (id == R.id.adcFastRadio)
                {
                    m.mAdc = ADC.FAST;
                }
                if (id == R.id.adcNormalRadio)
                {
                    m.mAdc = ADC.NORMAL;
                }
            }
        });

        mStartBut = v.findViewById(R.id.startBut);
        mStartBut.setOnClickListener(vi ->
        {
            if(mListener!=null)
            {
                ((MainActivity) mListener).gotoPlotNumWin();
            }
        });

        if(mListener!=null)
        {
            MainActivity m = (MainActivity)mListener;
            if(m.mAdc == ADC.NORMAL)
            {
                RadioButton b = v.findViewById(R.id.adcNormalRadio);
                b.setChecked(true);
            }
            if(m.mAdc == ADC.FAST)
            {
                RadioButton b = v.findViewById(R.id.adcFastRadio);
                b.setChecked(true);
            }
            if(m.mPg == PG.LOW_NOISE)
            {
                RadioButton b = v.findViewById(R.id.lowNoiseRadio);
                b.setChecked(true);
            }
            if(m.mPg == PG.DYNAMIC)
            {
                RadioButton b = v.findViewById(R.id.dynamicRadio);
                b.setChecked(true);
            }
        }

        return v;
    }


    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
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

    public void handleData(String data, String cmd)
    {
        try
        {
            if (cmd == "RM")
            {
                mParseCmd += data;
                int i = mParseCmd.indexOf("RD");
                if (i == -1)
                    return;
                mParseCmd = mParseCmd.substring(i);
                i = 0;
                int j = mParseCmd.indexOf("\r\n");
                if (j == -1)
                    return;

                String tarStr = mParseCmd.substring(i, j);
                tarStr.trim();

                if (mParseCmd.length() == j + 2)
                    mParseCmd = "";
                else
                    mParseCmd = mParseCmd.substring(j + 2);

                String[] parts = tarStr.split(",");
                Log.i("zhu","axi length:"+Integer.toString(parts.length));

                if(parts.length == 1)
                    mListener.onMainFragmentInteraction(CHelp.ASK_AXI, USBMAGAXI.ONE_AXIS);
                if(parts.length == 3)
                    mListener.onMainFragmentInteraction(CHelp.ASK_AXI,USBMAGAXI.THREE_AXIS);
            }
        }
        catch (Exception e)
        {
            Log.i("exp",e.toString());
            CHelp.showMsg(getContext(),e.toString());
        }
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
        void onMainFragmentInteraction(int what,Object obj);
    }
}
