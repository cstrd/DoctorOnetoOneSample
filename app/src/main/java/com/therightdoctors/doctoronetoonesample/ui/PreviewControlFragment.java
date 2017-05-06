package com.therightdoctors.doctoronetoonesample.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.therightdoctors.doctoronetoonesample.MainActivity;
import com.therightdoctors.doctoronetoonesample.R;
import com.tokbox.android.otsdkwrapper.listeners.SignalListener;
import com.tokbox.android.otsdkwrapper.signal.SignalInfo;
import com.tokbox.android.otsdkwrapper.utils.MediaType;


public class PreviewControlFragment extends Fragment implements SignalListener {
    
    private static final String LOGTAG = MainActivity.class.getName();

    private MainActivity mActivity;

    private View rootView;
    private ImageButton mAudioBtn;
    private ImageButton mVideoBtn;
    private ImageButton mCallBtn;

    private Drawable drawableStartCall;
    private  Drawable drawableEndCall;
    private  Drawable  drawableBckBtn;
    TextView timdura;
    private long startTime = 0L;
 
    private Handler customHandler = new Handler();
 
    long timeInMilliseconds = 0L;
 
    long timeSwapBuff = 0L;
 
    long updatedTime = 0L;
    String booking_id;
   Context c;

    private PreviewControlCallbacks mControlCallbacks = previewCallbacks;

  
    public interface PreviewControlCallbacks {

        void onDisableLocalAudio(boolean audio);

        void onDisableLocalVideo(boolean video);

        void onCall();
    }

    private static PreviewControlCallbacks previewCallbacks = new PreviewControlCallbacks() {
        @Override
        public void onDisableLocalAudio(boolean audio) { }

        @Override
        public void onDisableLocalVideo(boolean video) { }

        @Override
        public void onCall() { }

    };

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.localAudio:
                    updateLocalAudio();
                    break;

                case R.id.localVideo:
                    updateLocalVideo();
                    break;

                case R.id.call:
                    updateCall();
                    break;
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        Log.i(LOGTAG, "OnAttach PreviewControlFragment");

        super.onAttach(context);

        this.mActivity = (MainActivity) context;
        this.mControlCallbacks = (PreviewControlCallbacks) context;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            this.mActivity = (MainActivity) activity;
            this.mControlCallbacks = (PreviewControlCallbacks) activity;
        }
    }

    @Override
    public void onDetach() {
        Log.i(LOGTAG, "onDetach PreviewControlFragment");

        super.onDetach();

        mControlCallbacks = previewCallbacks;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(LOGTAG, "OnCreate PreviewControlFragment");
        
        rootView = inflater.inflate(R.layout.preview_actionbar_fragment, container, false);
        mAudioBtn = (ImageButton)rootView.findViewById(R.id.localAudio);
        mVideoBtn = (ImageButton)rootView.findViewById(R.id.localVideo);
        mCallBtn = (ImageButton)rootView.findViewById(R.id.call);
        timdura= (TextView)rootView.findViewById(R.id.timeduration); 
        c=mActivity;
        booking_id=mActivity.bookingId;
        drawableStartCall = AppCompatDrawableManager.get().getDrawable(mActivity,R.drawable.initiate_call_button);
        drawableEndCall =  AppCompatDrawableManager.get().getDrawable(mActivity,R.drawable.end_call_button);
        drawableBckBtn =   AppCompatDrawableManager.get().getDrawable(mActivity,R.drawable.bckg_icon);

        mAudioBtn.setImageResource(mActivity.getWrapper().isLocalMediaEnabled(MediaType.AUDIO)
                ? R.drawable.mic_icon
                : R.drawable.muted_mic_icon);
        mAudioBtn.setBackground(drawableBckBtn);

        mVideoBtn.setImageResource(mActivity.getWrapper().isLocalMediaEnabled(MediaType.VIDEO)
                ? R.drawable.video_icon
                : R.drawable.no_video_icon);
        mVideoBtn.setBackground(drawableBckBtn);

        mCallBtn.setImageResource(mActivity.isCallInProgress()
                ? R.drawable.hang_up
                : R.drawable.start_call);

        mCallBtn.setBackground(mActivity.isCallInProgress()
                ? drawableEndCall
                : drawableStartCall);

        mCallBtn.setOnClickListener(mBtnClickListener);

        setEnabled(mActivity.isCallInProgress());

        return rootView;
    }

    public void updateLocalAudio() {
        if (!mActivity.getWrapper().isLocalMediaEnabled(MediaType.AUDIO)) {
            mControlCallbacks.onDisableLocalAudio(true);
            mAudioBtn.setImageResource(R.drawable.mic_icon);
        } else {
            mControlCallbacks.onDisableLocalAudio(false);
            mAudioBtn.setImageResource(R.drawable.muted_mic_icon);
        }
    }

    public void updateLocalVideo() {
        if (!mActivity.getWrapper().isLocalMediaEnabled(MediaType.VIDEO)){
            mControlCallbacks.onDisableLocalVideo(true);
            mVideoBtn.setImageResource(R.drawable.video_icon);
        } else {
            mControlCallbacks.onDisableLocalVideo(false);
            mVideoBtn.setImageResource(R.drawable.no_video_icon);
        }
    }

    public void updateCall() {
    
        mCallBtn.setImageResource(!mActivity.isCallInProgress()
                ? R.drawable.hang_up
                : R.drawable.start_call);

        mCallBtn.setBackground(!mActivity.isCallInProgress()
                ? drawableEndCall
                : drawableStartCall);

        if ( mControlCallbacks != null ){
            mControlCallbacks.onCall();}
       mActivity.getWrapper().addSignalListener("Started", this);
        
        if(mActivity.isCallInProgress()){
           
            startTime = SystemClock.uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
           mActivity.getWrapper().getSession().sendSignal("Ended","hurrey");
          
     
        }else{
            timeSwapBuff += timeInMilliseconds;
            customHandler.removeCallbacks(updateTimerThread);

            mActivity.getWrapper().getSession().sendSignal("Ended","bye");
            mActivity.getWrapper().getSession().sendSignal("Started","bye");

        }
        
    }

    public void setEnabled(boolean enabled) {
        if (mVideoBtn != null && mAudioBtn != null) {
            if (enabled) {
                mAudioBtn.setOnClickListener(mBtnClickListener);
                mVideoBtn.setOnClickListener(mBtnClickListener);
            } else {
                mAudioBtn.setOnClickListener(null);
                mVideoBtn.setOnClickListener(null);
                mAudioBtn.setImageResource(R.drawable.mic_icon);
                mVideoBtn.setImageResource(R.drawable.video_icon);
            }
        }
    }

    public void restart() {
        setEnabled(false);
        mCallBtn.setBackground(drawableStartCall);
        mCallBtn.setImageResource(R.drawable.start_call);

    }
 
    private Runnable updateTimerThread = new Runnable() {
 
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            timdura.setText("Time: " + mins + ":" + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
            
        }
 
    };
    @Override
    public void onSignalReceived(SignalInfo signalInfo, boolean b) {
        if(!b){
            String data= (String) signalInfo.mData;
          String destid=signalInfo.mDstConnId;
          System.out.println(data+""+destid);
            // Toast.makeText(mActivity, data, Toast.LENGTH_SHORT).show();
            if(data.equals("bye")){
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity, "Patient has Disconnected", Toast.LENGTH_SHORT).show();
                        
                        final Dialog dialog = new Dialog(mActivity);
                        dialog.setContentView(R.layout.custom);

                        TextView set=(TextView)dialog.findViewById(R.id.nodata);
                       set.setText("your Patient has Disconnected your Consultation completed. the time duation are sent to server ");
                        // Button cancel=(Button)dialog.findViewById(R.id.cancel);
                        Button ok=(Button)dialog.findViewById(R.id.ok);
                   /*cancel.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           dialog.dismiss();
                       }
                   });*/
                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

 
                                
              /*                  
                                try {
                                    JSONObject jbj = new JSONObject();

                                    jbj.put("booking_flag","completed");

                                  final String payload=jbj.toString();

                                    RequestQueue queue = Volley.newRequestQueue(mActivity);
                                    System.out.println(booking_id);
                                    String url = "https://therightdoctors.com/api/beta/opm/patient/video_consultation_booking/endcall/"+booking_id+"?token=trd_token&key=7xOyNH554tY83cBN7Ktpw3s1y68ql6Eg";
                                    StringRequest putRequest = new StringRequest(Request.Method.PUT, url,
                                            new Response.Listener<String>()
                                            {
                                                @Override
                                                public void onResponse(String response) {
                                                    // response
                                                    Log.d("Response", response);
                                                    Toast.makeText(c,response+ "--your Consultation have updated in server", Toast.LENGTH_SHORT).show();
                                                    mActivity.getWrapper().disconnect();
                                                    mActivity.finish();
                                                }
                                            },
                                            new Response.ErrorListener()
                                            {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    // error
                                                    Log.d("Error.Response","response eroro" );
                                                    if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                                                        Snackbar snackbar = Snackbar
                                                                .make(dialog.findViewById(android.R.id.content), "TimeOut or no Connection", Snackbar.LENGTH_LONG);
                                                        snackbar.setDuration(5000);
                                                        snackbar.show();
                                                    } else if (error instanceof AuthFailureError) {
                                                        //TODO
                                                        Snackbar snackbar = Snackbar
                                                                .make(dialog.findViewById(android.R.id.content), "AuthFailure try again with Correct Credentials", Snackbar.LENGTH_LONG);
                                                        snackbar.setDuration(5000);
                                                        snackbar.show();
                                                    } else if (error instanceof ServerError) {
                                                        //TODO
                                                        Snackbar snackbar = Snackbar
                                                                .make(dialog.findViewById(android.R.id.content), "Server Error Please Try After Some time", Snackbar.LENGTH_LONG);
                                                        snackbar.setDuration(5000);
                                                        snackbar.show();
                                                    } else if (error instanceof NetworkError) {
                                                        //TODO
                                                        Snackbar snackbar = Snackbar
                                                                .make(dialog.findViewById(android.R.id.content), "Please the check the Network", Snackbar.LENGTH_LONG);
                                                        snackbar.setDuration(5000);
                                                        snackbar.show();
                                                    } else if (error instanceof ParseError) {
                                                        //TODO
                                                        Snackbar snackbar = Snackbar
                                                                .make(dialog.findViewById(android.R.id.content), "Parse Error", Snackbar.LENGTH_LONG);
                                                        snackbar.setDuration(5000);
                                                        snackbar.show();
                                                    }
                                                }
                                                // sdaasf
                                            }){
                                        @Override
                                        public Map<String, String> getHeaders() {
                                            Map<String, String> pars = new HashMap<String, String>();
                                            pars.put("Content-Type", "application/x-www-form-urlencoded");
                                            return pars;
                                        }

                                        @Override
                                        protected Map<String, String> getParams() {
                                            Map<String, String> params = new HashMap<String, String>();
                                            params.put("json",payload);
                                            System.out.println(params);
                                            return params;
                                        }
                                    };


                                    putRequest.setRetryPolicy(new DefaultRetryPolicy(3*DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 0));
                                    putRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
                                    // Adds the JSON object request "obreq" to the request queue
                                    queue.add(putRequest);



                                }catch(JSONException e){
                                    e.printStackTrace();
                                }*/
                                dialog.dismiss();
                              
                            }
                        });
                        dialog.show();
                        

                    }
                });

            }
        }
    }


}