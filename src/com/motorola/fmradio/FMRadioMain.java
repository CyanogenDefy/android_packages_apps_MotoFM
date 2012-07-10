package com.motorola.fmradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.fmradio.FMDataProvider.Channels;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FMRadioMain extends Activity implements SeekBar.OnSeekBarChangeListener,
       View.OnClickListener, View.OnLongClickListener,
       View.OnTouchListener, ImageSwitcher.ViewFactory
{
    private static final String TAG = "FMRadioMain";

    private static int LIGHT_ON_TIME = 90000;
    private static int PRESET_NUM = 20;

    private static final int DIALOG_PROGRESS = 0;
    private static final int DIALOG_SCAN_FINISH = 1;
    private static final int DIALOG_SCAN_CANCEL = 2;
    private static final int DIALOG_IF_SCAN_FIRST = 3;
    private static final int DIALOG_IF_SCAN_NEXT = 4;
    private static final int DIALOG_SAVE_CHANNEL = 5;
    private static final int DIALOG_EDIT_CHANNEL = 6;

    private static final int SHORTPRESS_BUTTON_NONE = 0;
    private static final int SHORTPRESS_BUTTON_ADD = 1;
    private static final int SHORTPRESS_BUTTON_REDUCE = 2;

    private static final int LONGPRESS_BUTTON_NONE = 0;
    private static final int LONGPRESS_BUTTON_ADD = 1;
    private static final int LONGPRESS_BUTTON_REDUCE = 2;

    public static final int PLAY_MENU_ID = 1;
    public static final int EDIT_MENU_ID = 2;
    public static final int REPLACE_MENU_ID = 3;
    public static final int CLEAR_MENU_ID = 4;

    public static final int SAVE_ID = 1;
    public static final int EDIT_ID = 2;
    public static final int CLEAR_ID = 3;
    public static final int EXIT_ID = 4;
    public static final int SCAN_SAVE_ID = 5;
    public static final int BY_LOUDSPEAKER_ID = 6;
    public static final int BY_HEADSET_ID = 7;

    private static final int SAVE_CODE = 0;
    private static final int EDIT_CODE = 1;
    private static final int CLEAR_CODE = 2;

    private static final int START_FMRADIO = 1;
    private static final int STOP_FMRADIO = 2;
    private static final int QUIT = 3;
    private static final int FM_OPEN_FAILED = 4;
    private static final int FM_TUNE_SUCCEED = 6;
    private static final int FM_SEEK_SUCCEED = 7;
    private static final int FM_SEEK_FAILED = 8;
    private static final int FM_SEEK_SUCCEED_AND_REACHLIMIT = 9;
    private static final int FM_HW_ERROR_UNKNOWN = 10;
    private static final int FM_HW_ERROR_FRQ = 11;
    private static final int SEEK_NEXT = 12;
    private static final int FM_RDS_DATA_AVAILABLE = 13;
    private static final int FM_FREQ_ADD = 14;
    private static final int FM_FREQ_REDUCE = 15;
    private static final int FM_SCAN_SUCCEED = 16;
    private static final int FM_SCANNING = 17;
    private static final int FM_SCAN_FAILED = 18;
    private static final int FM_ABORT_COMPLETE = 19;

    private static final int SAVED_STATE_INDEX_LASTCHNUM = 1;
    private static final int SAVED_STATE_INDEX_LASTFREQ = 2;

    private int RANGE = 21000;
    private int RANGE_START = 87000;
    private int RATE = 1000;

    private static final String RDS_TEXT_SEPARATOR = "..:";

    private static final int FREQ_RATE = 1000;

    private static final int LONG_PRESS_SEEK_TIMEOUT = 3000;
    private static final int LONG_PRESS_TUNE_TIMEOUT = 50;
    protected static final long SCAN_STOP_DELAY = 1000;

    private static final int[] NUMBER_IMAGES = new int[] {
        R.drawable.fm_number_0, R.drawable.fm_number_1, R.drawable.fm_number_2,
        R.drawable.fm_number_3, R.drawable.fm_number_4, R.drawable.fm_number_5,
        R.drawable.fm_number_6, R.drawable.fm_number_7, R.drawable.fm_number_8,
        R.drawable.fm_number_9
    };
    private static final int[] NUMBER_IMAGES_UNSELECTED = new int[] {
        R.drawable.fm_number_unselect_0, R.drawable.fm_number_unselect_1,
        R.drawable.fm_number_unselect_2, R.drawable.fm_number_unselect_3,
        R.drawable.fm_number_unselect_4, R.drawable.fm_number_unselect_5,
        R.drawable.fm_number_unselect_6, R.drawable.fm_number_unselect_7,
        R.drawable.fm_number_unselect_8, R.drawable.fm_number_unselect_9
    };
    private static final int[] NUMBER_IMAGES_PRESET = new int[] {
        R.drawable.fm_playing_list_0, R.drawable.fm_playing_list_1,
        R.drawable.fm_playing_list_2, R.drawable.fm_playing_list_3,
        R.drawable.fm_playing_list_4, R.drawable.fm_playing_list_5,
        R.drawable.fm_playing_list_6, R.drawable.fm_playing_list_7,
        R.drawable.fm_playing_list_8, R.drawable.fm_playing_list_9
    };
    private static final int[] PTY_STRINGS = new int[] {
        R.string.fm_pty_list_00, R.string.fm_pty_list_01, R.string.fm_pty_list_02,
        R.string.fm_pty_list_03, R.string.fm_pty_list_04, R.string.fm_pty_list_05,
        R.string.fm_pty_list_06, R.string.fm_pty_list_07, R.string.fm_pty_list_08,
        R.string.fm_pty_list_09, R.string.fm_pty_list_10, R.string.fm_pty_list_11,
        R.string.fm_pty_list_12, R.string.fm_pty_list_13, R.string.fm_pty_list_14,
        R.string.fm_pty_list_15, R.string.fm_pty_list_16, R.string.fm_pty_list_17,
        R.string.fm_pty_list_18, R.string.fm_pty_list_19, R.string.fm_pty_list_20,
        R.string.fm_pty_list_21, R.string.fm_pty_list_22, R.string.fm_pty_list_23,
        R.string.fm_pty_list_24, R.string.fm_pty_list_25, R.string.fm_pty_list_26,
        R.string.fm_pty_list_27, R.string.fm_pty_list_28, R.string.fm_pty_list_29,
        R.string.fm_pty_list_30, R.string.fm_pty_list_31, R.string.fm_pty_list_32,
        R.string.fm_pty_list_33, R.string.fm_pty_list_34, R.string.fm_pty_list_35,
        R.string.fm_pty_list_36, R.string.fm_pty_list_37, R.string.fm_pty_list_38,
        R.string.fm_pty_list_39, R.string.fm_pty_list_40, R.string.fm_pty_list_41,
        R.string.fm_pty_list_42, R.string.fm_pty_list_43, R.string.fm_pty_list_44,
        R.string.fm_pty_list_45, R.string.fm_pty_list_46, R.string.fm_pty_list_47,
        R.string.fm_pty_list_48, R.string.fm_pty_list_49, R.string.fm_pty_list_50,
        R.string.fm_pty_list_51, R.string.fm_pty_list_52, R.string.fm_pty_list_53,
        R.string.fm_pty_list_54, R.string.fm_pty_list_55, R.string.fm_pty_list_56,
        R.string.fm_pty_list_57, R.string.fm_pty_list_58, R.string.fm_pty_list_59,
        R.string.fm_pty_list_60, R.string.fm_pty_list_61, R.string.fm_pty_list_62,
        R.string.fm_pty_list_63
    };

    private ImageButton[] mSeekButtons;
    private ImageSwitcher[] mFreqDigits;
    private ImageSwitcher[] mPresetDigits;
    private RelativeLayout mPresetLayout;
    private SeekBar mSeekBar;
    private MarqueeText mRdsMarqueeText;
    private ImageView mScanBar;
    private AnimationDrawable mScanAnimation;

    private Cursor mCursor;
    private ListView mChannelList;

    private boolean mIsBound = false;
    private WakeLock mWakeLock;
    private AudioManager mAM;

    private String mRdsStationName;
    private String mRdsRadioText;
    private int mRdsPTYValue;

    private int count_save = 0;
    private ImageView currentIcon = null;
    private boolean isEdit = true;
    protected boolean isInitial = true;
    private boolean isScanAll = false;
    private boolean isScanCanceled = false;
    private boolean isSeekStarted = false;
    private ImageView lastIcon = null;
    private int mCurFreq = FMUtil.MIN_FREQUENCY;
    private int mFirstCounter = FMUtil.MIN_FREQUENCY;
    private boolean mInitSuccess = true;
    private int mLongPressButtonType = LONGPRESS_BUTTON_NONE;
    private int mPreFreq = FMUtil.MIN_FREQUENCY;
    private int mRDSFreq = 0;
    private IFMRadioPlayerService mService = null;
    private int mShortPressButtonType = SHORTPRESS_BUTTON_NONE;
    private Timer mTimer;
    private TimerTask mTimerTask;
    protected boolean mTuneSucceed = true;
    private boolean mbIsLongPressed = false;
    private ProgressDialog pDialog;
    private ProgressDialog pDialog_waitpoweron = null;

    private class ScanStopThread extends Thread {
        @Override
        public void run() {
            super.run();
            if (mScanBar.getBackground() != null) {
                mScanAnimation.stop();
                mScanBar.setBackgroundDrawable(null);
                enableUI(true);
            }
        }
    }

    private class ChannelListAdapter extends ResourceCursorAdapter {
        private class ViewHolder {
            private ImageView mIcon;
            private TextView mChannel;
            private TextView mName;

            public ViewHolder(View view) {
                mChannel = (TextView) view.findViewById(R.id.list_text);
                mName = (TextView) view.findViewById(R.id.list_text2);
                mIcon = (ImageView) view.findViewById(R.id.list_icon);
            }

            public void bind(Context context, Cursor cursor) {
                int id = cursor.getInt(FMUtil.CHANNEL_COLUMN_ID);
                boolean selected = cursor.getPosition() == mChannelList.getCheckedItemPosition();

                mIcon.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                mChannel.setText(String.format("%02d", id + 1));
                mName.setText(FMUtil.getPresetListString(context, cursor));
            }
        }

        public ChannelListAdapter(Context context, Cursor cursor) {
            super(context, R.layout.listview_row, cursor);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder(view);
                view.setTag(holder);
            }
            holder.bind(context, cursor);
        }
    }

    private ServiceConnection mServConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected::fmradio java service started");
            mService = IFMRadioPlayerService.Stub.asInterface(service);

            boolean bPowerStatus = false;
            if (mService != null) {
                try {
                    mService.registerCallbacks(mServiceCallbacks);
                } catch (RemoteException e) {
                    Log.e(TAG, "Could not register with service");
                    unbindFMRadioService();
                    return;
                }
                try {
                    bPowerStatus = mService.isPowerOn();
                } catch (RemoteException e) {
                    Log.e(TAG, "Justfy if chip power on failed");
                }
            }
            if (!bPowerStatus) {
                if (pDialog_waitpoweron == null) {
                    pDialog_waitpoweron = ProgressDialog.show(FMRadioMain.this, "", getResources().getText(R.string.fmradio_waiting_for_power_on), true, true);
                    Log.w(TAG, "servie is ready popup a wait dialog");
                }
            } else  if (!Preferences.isEnabled(FMRadioMain.this)) {
                mHandler.sendEmptyMessage(START_FMRADIO);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "From FM ui fmradio service layer disconnected");
            mService = null;
            finish();
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Context context = FMRadioMain.this;

            switch (msg.what) {
                case START_FMRADIO:
                    // pswitch_0
                    enableUI(true);
                    Preferences.setEnabled(context, true);
                    mAM.setStreamVolume(AudioManager.STREAM_FM, Preferences.getVolume(context), 0);
                    if (isDBEmpty() || !Preferences.isScanned(context)) {
                        showDialog(DIALOG_IF_SCAN_FIRST);
                    }
                    break;
                case STOP_FMRADIO:
                    // pswitch_1
                    powerOffFMRadioDevice();
                    break;
                case QUIT:
                    // pswitch_2
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                    break;
                case FM_OPEN_FAILED:
                    // pswitch_3
                    Preferences.setEnabled(context, false);
                    Toast.makeText(context, "FMRadio Open failed", Toast.LENGTH_SHORT).show();
                    break;
                case FM_TUNE_SUCCEED:
                    // pswitch_5
                    displayRdsScrollByCurFreq(false);
                    enableUI(true);
                    ignoreRdsEvent(false);
                    mTuneSucceed = true;
                    updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                    Log.d(TAG, "FM Tune succeed callback");
                    break;
                case FM_SEEK_FAILED:
                case FM_SCAN_FAILED:
                    // pswitch_6
                    if (isScanAll) {
                        mHandler.sendEmptyMessage(SEEK_NEXT);
                        isScanCanceled = true;
                    } else {
                        ScanStopThread sThread = new ScanStopThread();
                        mHandler.postDelayed(sThread, SCAN_STOP_DELAY);
                    }
                    break;
                case FM_SEEK_SUCCEED:
                case FM_SCANNING:
                    // pswitch_7
                    Log.d(TAG, "seek forward/backward callback, the new frequency = " + mCurFreq);
                    isSeekStarted = false;
                    mShortPressButtonType = SHORTPRESS_BUTTON_NONE;
                    if (mbIsLongPressed) {
                        resetTimer();
                        mPreFreq = mCurFreq;
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        mTimer = new Timer();
                        if (mLongPressButtonType == LONGPRESS_BUTTON_REDUCE) {
                            mTimerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    isSeekStarted = true;
                                    mLongPressButtonType = LONGPRESS_BUTTON_REDUCE;
                                    seekFMRadioStation(0, 1);
                                }
                            };
                            mTimer.schedule(mTimerTask, LONG_PRESS_SEEK_TIMEOUT);
                        } else if (mLongPressButtonType == LONGPRESS_BUTTON_ADD) {
                            mTimerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    isSeekStarted = true;
                                    mLongPressButtonType = LONGPRESS_BUTTON_ADD;
                                    seekFMRadioStation(0, 0);
                                }
                            };
                        } else {
                            Log.e(TAG, "Error, Long press seek type is unknonw;");
                        }
                    } else if (isScanAll) {
                        if (count_save <= PRESET_NUM) {
                            saveStationToDB(count_save, mCurFreq, "", "");
                            if (count_save == 0) {
                                mFirstCounter = mCurFreq;
                            }
                            count_save++;
                            if (pDialog != null) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(getString(R.string.fmradio_found));
                                sb.append(" ");
                                sb.append(count_save);
                                sb.append(" ");
                                sb.append(getString(count_save > 1 ? R.string.fmradio_stations : R.string.fmradio_station));
                                sb.append("...");
                                pDialog.setMessage(sb.toString());
                            }
                            if (!isScanCanceled && count_save >= PRESET_NUM) {
                                pDialog.cancel();
                            }
                        }
                    }
                    if (!isScanAll || isScanCanceled) {
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        displayRdsScrollByCurFreq(false);
                        enableUI(true);
                        ScanStopThread sThread = new ScanStopThread();
                        mHandler.postDelayed(sThread, SCAN_STOP_DELAY);
                    }
                    break;
                case SEEK_NEXT:
                    // pswitch_8
                    Log.d(TAG, "SEEK_NEXT received");
                    if (count_save < PRESET_NUM && !isScanCanceled) {
                        Log.d(TAG, "scan not canceled, seek next station");
                        seekFMRadioStation(0, 0);
                    } else {
                        Log.d(TAG, "20 stations scaned out");
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append(getString(R.string.fmradio_save_canceled));
                        sb.append(" ");
                        sb.append(getString(R.string.saved));
                        sb.append(" ");
                        sb.append(count_save);
                        sb.append(" ");
                        sb.append(getString(count_save > 1 ? R.string.fmradio_stations : R.string.fmradio_station));
                        sb.append(".");
                        Toast ts = Toast.makeText(FMRadioMain.this, sb.toString(), Toast.LENGTH_SHORT);
                        ts.setGravity(Gravity.CENTER, 0, 0);
                        ts.show();

                        ScanStopThread sThread = new ScanStopThread();
                        mHandler.postDelayed(sThread, SCAN_STOP_DELAY);

                        if (lastIcon != null) {
                            lastIcon.setVisibility(View.INVISIBLE);
                        }
                        setSelectedPreset(-1);
                        count_save = 0;
                        isScanAll = false;
                    }
                    break;
                case FM_SEEK_SUCCEED_AND_REACHLIMIT:
                case FM_SCAN_SUCCEED:
                    Log.d(TAG, "seek reach limit, mCurFreq = " + mCurFreq);
                    if (isScanAll) {
                        isScanCanceled = true;
                        mCurFreq = mFirstCounter;
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append(getString(R.string.saved));
                        sb.append(" ");
                        sb.append(count_save);
                        sb.append(" ");
                        sb.append(getString(count_save > 1 ? R.string.fmradio_stations : R.string.fmradio_station));
                        sb.append(".");
                        Toast ts = Toast.makeText(FMRadioMain.this, sb.toString(), Toast.LENGTH_SHORT);
                        ts.setGravity(Gravity.CENTER, 0, 0);
                        ts.show();
                        if (lastIcon != null) {
                            lastIcon.setVisibility(View.INVISIBLE);
                        }
                        ignoreRdsEvent(false);
                        setSelectedPreset(-1);
                        count_save = 0;
                        isScanAll = false;
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        displayRdsScrollByCurFreq(false);

                        ScanStopThread sThread = new ScanStopThread();
                        mHandler.postDelayed(sThread, SCAN_STOP_DELAY);
                        Log.d(TAG, "seek forward/backward reach limit callback, the new frequency = " + mCurFreq);
                    } else if (isSeekStarted || mbIsLongPressed) {
                        if (mLongPressButtonType == LONGPRESS_BUTTON_REDUCE) {
                            isSeekStarted = true;
                            seekFMRadioStation(FMUtil.MAX_FREQUENCY, 1);
                        } else if (mLongPressButtonType == LONGPRESS_BUTTON_ADD) {
                            isSeekStarted = true;
                            seekFMRadioStation(FMUtil.MIN_FREQUENCY, 0);
                        } else {
                            Log.e(TAG, "Error, Long press seek type is unknonw;");
                        }
                    } else if (isSeekStarted) {
                        isSeekStarted = false;
                        if (mShortPressButtonType == SHORTPRESS_BUTTON_ADD) {
                            seekFMRadioStation(FMUtil.MIN_FREQUENCY, 0);
                        } else if (mShortPressButtonType == SHORTPRESS_BUTTON_REDUCE) {
                            seekFMRadioStation(FMUtil.MAX_FREQUENCY, 1);
                        }
                    } else {
                        mCurFreq = mPreFreq;
                        setFMRadioFrequency();
                        isScanAll = false;
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        displayRdsScrollByCurFreq(false);

                        ScanStopThread sThread = new ScanStopThread();
                        mHandler.postDelayed(sThread, SCAN_STOP_DELAY);
                        Log.d(TAG, "seek forward/backward reach limit callback, the new frequency = " + mCurFreq);
                    }
                    break;
                case FM_HW_ERROR_UNKNOWN:
                    Log.d(TAG, "FM hardware unknow error!");
                    enableUI(true);
                    break;
                case FM_HW_ERROR_FRQ:
                    Log.d(TAG, "FM Tune frequency error!");
                    enableUI(true);
                    break;
                case FM_RDS_DATA_AVAILABLE:
                    Log.d(TAG, "FM RDS data available!");
                    if (!TextUtils.isEmpty(mRdsStationName)) {
                        Log.d(TAG, "station name available");
                        Cursor cursor = getContentResolver().query(Channels.CONTENT_URI, FMUtil.PROJECTION,
                                Channels.FREQUENCY + "=?", new String[] { String.valueOf(mCurFreq) }, null);
                        if (cursor != null && cursor.getCount() > 0) {
                            Log.d(TAG, "checking each preset");
                            cursor.moveToFirst();
                            while (!cursor.isAfterLast()) {
                                Log.d(TAG, "getting cursor string");
                                String chName = cursor.getString(FMUtil.CHANNEL_COLUMN_NAME);
                                if (FMUtil.isEmptyStr(chName)) {
                                    int id = cursor.getInt(FMUtil.CHANNEL_COLUMN_ID);
                                    saveStationToDB(id, mCurFreq, null, mRdsStationName);
                                    updateDisplayPanel(mCurFreq, true);
                                    updatePresetSwitcher(id + 1);
                                }
                                cursor.moveToNext();
                            }
                            cursor.close();
                        }
                    }
                    StringBuilder rdsText = new StringBuilder();
                    if (!TextUtils.isEmpty(mRdsStationName)) {
                        rdsText.append(mRdsStationName);
                    }
                    if (!TextUtils.isEmpty(mRdsRadioText)) {
                        if (rdsText.length() > 0) {
                            rdsText.append(RDS_TEXT_SEPARATOR);
                        }
                        rdsText.append(mRdsRadioText);
                    }
                    if (mRdsPTYValue >= 0 && mRdsPTYValue < PTY_STRINGS.length) {
                        if (rdsText.length() > 0) {
                            rdsText.append(RDS_TEXT_SEPARATOR);
                        }
                        rdsText.append(getString(PTY_STRINGS[mRdsPTYValue]));
                    }
                    mRdsMarqueeText.setText(rdsText.toString());
                    displayRdsScrollByCurFreq(true);
                    break;
                case FM_FREQ_ADD:
                    if (mbIsLongPressed) {
                        resetTimer();
                        mCurFreq += FMUtil.STEP;
                        if (mCurFreq > FMUtil.MAX_FREQUENCY) {
                            mCurFreq = FMUtil.MIN_FREQUENCY;
                        }
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        mTimer = new Timer();
                        mTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                mHandler.sendEmptyMessage(FM_FREQ_ADD);
                            }
                        };
                        mTimer.schedule(mTimerTask, LONG_PRESS_TUNE_TIMEOUT);
                    }
                    break;
                case FM_FREQ_REDUCE:
                    if (mbIsLongPressed) {
                        resetTimer();
                        mCurFreq -= FMUtil.STEP;
                        if (mCurFreq < FMUtil.MIN_FREQUENCY) {
                            mCurFreq = FMUtil.MAX_FREQUENCY;
                        }
                        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                        mTimer = new Timer();
                        mTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                mHandler.sendEmptyMessage(FM_FREQ_REDUCE);
                            }
                        };
                        mTimer.schedule(mTimerTask, LONG_PRESS_TUNE_TIMEOUT);
                    }
                    break;
            }
        }
    };

    private IFMRadioPlayerServiceCallbacks.Stub mServiceCallbacks = new IFMRadioPlayerServiceCallbacks.Stub() {
        @Override
        public void onEnabled(boolean success) {
            if (success) {
                Log.w(TAG, "Real FM power on success.");
                if (pDialog_waitpoweron != null) {
                    Log.w(TAG, "poweron success, dismiss waitting dialog");
                    pDialog_waitpoweron.dismiss();
                    pDialog_waitpoweron = null;
                }
                if (!Preferences.isEnabled(FMRadioMain.this)) {
                    mHandler.sendEmptyMessage(START_FMRADIO);
                }
            } else {
                mHandler.sendEmptyMessage(FM_OPEN_FAILED);
            }
        }

        @Override
        public void onDisabled() {
            finish();
        }

        @Override
        public void onTuneChanged(boolean success) {
            mHandler.sendEmptyMessage(success ? FM_TUNE_SUCCEED : FM_HW_ERROR_FRQ);
        }

        @Override
        public void onSeekFinished(boolean success, int newFrequency) {
            if (success) {
                boolean reachedLimit = mCurFreq == newFrequency &&
                        (newFrequency == FMUtil.MIN_FREQUENCY || newFrequency == FMUtil.MAX_FREQUENCY);
                mCurFreq = newFrequency;
                mHandler.sendEmptyMessage(reachedLimit ? FM_SEEK_SUCCEED_AND_REACHLIMIT : FM_SEEK_SUCCEED);
            } else {
                mHandler.sendEmptyMessage(FM_SEEK_FAILED);
            }
        }

        @Override
        public void onScanUpdate(int newFrequency) {
            mCurFreq = newFrequency;
            mHandler.sendEmptyMessage(FM_SCANNING);
        }

        @Override
        public void onScanFinished(boolean success, int newFrequency) {
            if (success) {
                mCurFreq = newFrequency;
            }
            mHandler.sendEmptyMessage(FM_SCAN_SUCCEED);
        }

        @Override
        public void onAbortComplete() {
            if (isScanCanceled) {
                if (count_save < PRESET_NUM) {
                    mHandler.sendEmptyMessage(SEEK_NEXT);
                } else {
                    mHandler.sendEmptyMessage(FM_SCAN_SUCCEED);
                }
            }
        }

        @Override
        public void onError() {
            mHandler.sendEmptyMessage(FM_HW_ERROR_UNKNOWN);
        }

        @Override
        public void onRdsDataChanged(int frequency, String stationName, String radioText, int pty) {
            mRDSFreq = frequency;
            mRdsStationName = stationName;
            mRdsRadioText = radioText.replaceAll("\n", " ");
            mRdsPTYValue = pty;
            mHandler.sendEmptyMessage(FM_RDS_DATA_AVAILABLE);
        }

        @Override
        public void onAudioModeChanged(int newMode) {
            updateStereoStatus();
        }
    };

    private boolean bindToService() {
        Log.d(TAG, "Start to bind to FMRadio service");
        Intent i = new Intent(this, FMRadioPlayerService.class);
        startService(i);
        return bindService(i, mServConnection, 0);
    }

    private void unbindFMRadioService() {
        if (mIsBound) {
            try {
                mService.unregisterCallbacks();
            } catch (RemoteException e) {
            }
            unbindService(mServConnection);
            mIsBound = false;
        }
        stopService(new Intent(this, FMRadioPlayerService.class));
    }

    private void clearPresetSwitcher() {
        mPresetLayout.setBackgroundDrawable(null);
        mPresetDigits[0].setImageDrawable(null);
        mPresetDigits[1].setImageDrawable(null);
    }

    private void displayRdsScrollByCurFreq(boolean show) {
        if (mCurFreq != mRDSFreq) {
            Log.w(TAG, "Current station is not the frequency where RDS will be shown");
            Log.w(TAG, "mRDSFreq = " + mRDSFreq + " mCurFreq = " + mCurFreq);
            displayRdsScrollText(false);
        } else if (show) {
            displayRdsScrollText(true);
        }
    }

    private void displayRdsScrollText(boolean show) {
        Log.d(TAG, "displayRdsScrollText(" + show + ")");
        if (!show) {
            mRdsStationName = null;
            mRdsRadioText = null;
            mRdsPTYValue = 0;
        }
        if (mSeekBar != null && mRdsMarqueeText != null) {
            if (show) {
                mSeekBar.setVisibility(View.GONE);
                mRdsMarqueeText.setVisibility(View.VISIBLE);
            } else {
                mSeekBar.setVisibility(View.VISIBLE);
                mRdsMarqueeText.setVisibility(View.GONE);
            }
        }
    }

    private void enableActiveButton(View v) {
        long id = v.getId();
        for (ImageButton button : mSeekButtons) {
            button.setEnabled(button.getId() == id);
        }
        updateButtonDrawables();
        mChannelList.setEnabled(false);
    }

    private void updateButtonDrawables() {
        for (ImageButton button : mSeekButtons) {
            boolean enabled = button.isEnabled();
            int resId = 0;

            switch (button.getId()) {
                case R.id.btn_seekbackward:
                    resId = enabled ? R.drawable.fm_autosearch_reduce_enable : R.drawable.fm_autosearch_reduce_disable;
                    break;
                case R.id.btn_reduce:
                    resId = enabled ? R.drawable.fm_manualadjust_reduce_enable : R.drawable.fm_manualadjust_reduce_disable;
                    break;
                case R.id.btn_add:
                    resId = enabled ? R.drawable.fm_manualadjust_plus_enable : R.drawable.fm_manualadjust_plus_disable;
                    break;
                case R.id.btn_seekforward:
                    resId = enabled ? R.drawable.fm_autosearch_plus_enable : R.drawable.fm_autosearch_plus_disable;
                    break;
            }
            button.setBackgroundResource(resId);
        }
    }

    private void enableUI(boolean enabled) {
        for (ImageButton button : mSeekButtons) {
            button.setEnabled(enabled);
        }
        updateButtonDrawables();
        if (mChannelList != null) {
            mChannelList.setEnabled(enabled);
        }
    }

    private int getIndexOfEmptyItem() {
        Cursor cursor = getContentResolver().query(Channels.CONTENT_URI, FMUtil.PROJECTION, null, null, null);
        int count = 0;

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getInt(FMUtil.CHANNEL_COLUMN_FREQ) == 0) {
                    break;
                }
                count++;
                cursor.moveToNext();
            }
            cursor.close();
        }

        return count;
    }

    private void handleSeekMsg(View v, int direction) {
        isEdit = false;
        isScanAll = false;
        isSeekStarted = true;
        mPreFreq = mCurFreq;
        seekFMRadioStation(0, direction);
        mScanBar.setBackgroundDrawable(mScanAnimation);
        mScanAnimation.start();
        enableActiveButton(v);
        displayRdsScrollText(false);
    }

    private void ignoreRdsEvent(boolean flag) {
        if (mService != null) {
            try {
                mService.ignoreRdsEvent(flag);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void initButtons() {
        mSeekButtons = new ImageButton[4];
        mSeekButtons[0] = (ImageButton) findViewById(R.id.btn_seekbackward);
        mSeekButtons[1] = (ImageButton) findViewById(R.id.btn_reduce);
        mSeekButtons[2] = (ImageButton) findViewById(R.id.btn_add);
        mSeekButtons[3] = (ImageButton) findViewById(R.id.btn_seekforward);
        for (ImageButton button : mSeekButtons) {
            button.setOnClickListener(this);
            button.setOnLongClickListener(this);
            button.setOnTouchListener(this);
        }
    }

    private void initImageSwitcher() {
        mFreqDigits = new ImageSwitcher[5];
        mFreqDigits[0] = (ImageSwitcher) findViewById(R.id.Img_switcher1);
        mFreqDigits[1] = (ImageSwitcher) findViewById(R.id.Img_switcher2);
        mFreqDigits[2] = (ImageSwitcher) findViewById(R.id.Img_switcher3);
        mFreqDigits[3] = (ImageSwitcher) findViewById(R.id.Img_switcher4);
        mFreqDigits[4] = (ImageSwitcher) findViewById(R.id.Img_switcher5);
        for (ImageSwitcher switcher : mFreqDigits) {
            switcher.setFactory(this);
        }

        mPresetLayout = (RelativeLayout) findViewById(R.id.preset_swt_layout);
        mPresetDigits = new ImageSwitcher[2];
        mPresetDigits[0] = (ImageSwitcher) findViewById(R.id.preset_swt1);
        mPresetDigits[1] = (ImageSwitcher) findViewById(R.id.preset_swt2);
        for (ImageSwitcher switcher : mPresetDigits) {
            switcher.setFactory(this);
        }
    }

    private void initListView() {
        mChannelList = (ListView) findViewById(R.id.channel_list);

        setSelectedPreset(Preferences.getLastChannel(this));

        mCursor = getContentResolver().query(Channels.CONTENT_URI, FMUtil.PROJECTION, null, null, null);
        if (mCursor == null) {
            Log.e(TAG, "Could not fetch channel data");
            mInitSuccess = false;
            return;
        }
        startManagingCursor(mCursor);

        mChannelList.setAdapter(new ChannelListAdapter(this, mCursor));
        mChannelList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuinfo) {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuinfo;
                int pos = (int) info.id;
                Uri uri = Uri.withAppendedPath(Channels.CONTENT_URI, String.valueOf(pos));
                Cursor cursor = getContentResolver().query(uri, FMUtil.PROJECTION, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    int frequency = cursor.getInt(FMUtil.CHANNEL_COLUMN_FREQ);
                    if (frequency == 0) {
                        saveChannel(pos);
                    } else {
                        menu.setHeaderTitle(FMUtil.getPresetListString(FMRadioMain.this, cursor));
                        if (mCurFreq != frequency) {
                            menu.add(Menu.NONE, PLAY_MENU_ID, Menu.FIRST, R.string.play_preset);
                            menu.add(Menu.NONE, REPLACE_MENU_ID, Menu.FIRST + 2, R.string.replace_preset);
                        }
                        menu.add(Menu.NONE, EDIT_MENU_ID, Menu.FIRST + 1, R.string.edit_preset);
                        menu.add(Menu.NONE, CLEAR_MENU_ID, Menu.FIRST + 3, R.string.clear_preset);
                    }
                    cursor.close();
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(getString(R.string.preset));
                    sb.append(" ");
                    sb.append(pos);
                    menu.setHeaderTitle(sb.toString());
                }
            }
        });
        mChannelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playClickPreset(position);
            }
        });
    }

    private void initResourceRefs() {
        Log.d(TAG, "enter initResourceRefs()");
        initImageSwitcher();
        initSeekBar();
        initButtons();
        initListView();
        enableUI(false);
        if (!mInitSuccess) {
            Toast t = Toast.makeText(this, R.string.error_mem_full, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            finish();
        } else {
            Log.d(TAG, "leave initResourceRefs()");
        }
    }

    private void initSeekBar() {
        mSeekBar = (SeekBar) findViewById(R.id.seek);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(true);

        mScanBar = (ImageView) findViewById(R.id.scan_anim);
        mScanAnimation = (AnimationDrawable) getResources().getDrawable(R.drawable.fm_progress_red);

        mRdsMarqueeText = (MarqueeText) findViewById(R.id.rds_text);
        mRdsMarqueeText.setTextColor(Color.WHITE);
        mRdsMarqueeText.setTextSize(22);
    }

    private void playClickPreset(int position) {
        Uri uri = Uri.withAppendedPath(Channels.CONTENT_URI, String.valueOf(position));
        Cursor cursor = getContentResolver().query(uri, FMUtil.PROJECTION, null, null, null);

        if (cursor == null) {
            Log.d(TAG, "not find item");
            return;
        }

        cursor.moveToFirst();

        int frequency = cursor.getInt(FMUtil.CHANNEL_COLUMN_FREQ);
        if (frequency == 0) {
            Log.d(TAG, "Select an empty list item, go to save UI");
            saveChannel(position);
        } else {
            if (lastIcon != null) {
                lastIcon.setVisibility(View.INVISIBLE);
            }
            currentIcon = (ImageView) findViewById(R.id.list_icon);
            currentIcon.setVisibility(View.VISIBLE);
            lastIcon = currentIcon;
            isEdit = true;
            mCurFreq = frequency;
            updateDisplayPanel(mCurFreq, true);
            updatePresetSwitcher(position + 1);
            setFMRadioFrequency();
        }
        cursor.close();
    }

    private void powerOffFMRadioDevice() {
        Log.d(TAG, "powerOffFMRadioDevice");
        if (mService != null) {
            try {
                Log.d(TAG, "mService.powerOff() called!");
                mService.powerOff();
            } catch (RemoteException e) {
                Log.d(TAG, "mService.powerOff() RemoteException!");
            }
        }
    }

    private void resetTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
            mTimerTask = null;
        }
    }

    private void scanFMRadioStation() {
        displayRdsScrollText(false);
        if (mService != null) {
            try {
                isScanAll = mService.scan();
            } catch (RemoteException e) {
                Log.d(TAG, "Calling mService.scan(): RemoteException.!");
                isScanAll = false;
            }
        }
        if (isScanAll) {
            showDialog(DIALOG_PROGRESS);
        } else {
            Log.d(TAG, "scan request failed");
            Toast t = Toast.makeText(this, R.string.error_start_scan, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    private void seekFMRadioStation(int freq, int direction) {
        Log.d(TAG, "seekFMRadioStation");
        displayRdsScrollText(false);
        if (mService != null) {
            try {
                mService.seek(freq, direction);
            } catch (RemoteException e) {
                Log.d(TAG, "In seekFMRadioStation(): RemoteException.!");
            }
        }
    }

    private void setFMRadioFrequency() {
        Log.d(TAG, "setFMRadioFrequency");
        displayRdsScrollText(false);
        if (mService != null) {
            try {
                mService.tune(mCurFreq);
            } catch (RemoteException e) {
                Log.d(TAG, "In setFMRadioFrequency(): RemoteException.!");
            }
        }
    }

    private void updateDisplayPanel(int currentFreq, boolean isEditEnable) {
        Log.d(TAG, "enter updateDisplayPanel()");
        float progress = currentFreq - RANGE_START;
        mSeekBar.setProgress((int) progress);

        updateFrequencyDisplay(currentFreq, isEditEnable);
        updateStereoStatus();
        Log.d(TAG, "leave updateDisplayPanel()");
    }

    private void updateFrequencyDisplay(int currentFreq, boolean isEditEnable) {
        if (currentFreq < FMUtil.MIN_FREQUENCY || currentFreq > FMUtil.MAX_FREQUENCY) {
            return;
        }

        int digit1, digit2, digit3, digit4, freq = currentFreq;

        digit1 = freq / 100000;
        freq -= digit1 * 100000;
        digit2 = freq / 10000;
        freq -= digit2 * 10000;
        digit3 = freq / 1000;
        freq -= digit3 * 1000;
        digit4 = freq / 100;

        Log.v(TAG, "FMRadio updateDisplay: currentFreq " + currentFreq + " -> digits " +
                digit1 + " " + digit2 + " " + digit3 + " " + digit4);

        int[] numbers = isEditEnable ? NUMBER_IMAGES : NUMBER_IMAGES_UNSELECTED;
        int dot = isEditEnable ? R.drawable.fm_number_point : R.drawable.fm_number_unselect_point;

        mFreqDigits[0].setImageResource(numbers[digit1]);
        mFreqDigits[0].setVisibility(digit1 == 0 ? View.INVISIBLE : View.VISIBLE);
        mFreqDigits[1].setImageResource(numbers[digit2]);
        mFreqDigits[2].setImageResource(numbers[digit3]);
        mFreqDigits[3].setImageResource(dot);
        mFreqDigits[4].setImageResource(numbers[digit4]);
    }

    private void updatePresetSwitcher(int index) {
        if (index <= 0 || index > PRESET_NUM) {
            return;
        }
        int index1 = index / 10;
        int index2 = index - (index1 * 10);

        mPresetLayout.setBackgroundResource(R.drawable.fm_playing_list_bg);
        mPresetDigits[0].setImageResource(NUMBER_IMAGES_PRESET[index1]);
        mPresetDigits[1].setImageResource(NUMBER_IMAGES_PRESET[index2]);
    }

    private boolean updatePresetSwitcher() {
        int index = -1;

        if (mCurFreq > 0) {
            Cursor cursor = getContentResolver().query(Channels.CONTENT_URI, FMUtil.PROJECTION,
                    Channels.FREQUENCY + "=?", new String[] { String.valueOf(mCurFreq) }, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    index = cursor.getInt(FMUtil.CHANNEL_COLUMN_ID);
                }
                cursor.close();
            }
        }

        if (index >= 0 && index < PRESET_NUM) {
            updatePresetSwitcher(index + 1);
            isEdit = true;
            setSelectedPreset(index);
        } else {
            clearPresetSwitcher();
            isEdit = false;
            setSelectedPreset(-1);
        }

        return index >= 0;
    }

    private void updateStereoStatus() {
        if (mService == null) {
            return;
        }
        ImageView imageStereoStatus = (ImageView) findViewById(R.id.stereo_status);
        if (imageStereoStatus == null) {
            return;
        }

        int mode = 0;
        try {
            mode = mService.getAudioMode();
        } catch (RemoteException e) {
            Log.e(TAG, "getAudioMode failed");
        }
        if (mode == 1) {
            imageStereoStatus.setVisibility(View.VISIBLE);
        } else if (mode == 0) {
            imageStereoStatus.setVisibility(View.INVISIBLE);
        }
    }

    public void clearDB() {
        for (int i = 0; i < PRESET_NUM; i++) {
            saveStationToDB(i, 0, "", "");
        }
    }

    public boolean isDBEmpty() {
        Cursor cursor = getContentResolver().query(Channels.CONTENT_URI, FMUtil.PROJECTION, null, null, null);
        boolean empty = true;

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getInt(FMUtil.CHANNEL_COLUMN_FREQ) != 0) {
                    empty = false;
                    break;
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return empty;
    }

    @Override
    public View makeView() {
        ImageView i = new ImageView(this);
        i.setScaleType(ScaleType.CENTER_INSIDE);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(-1, -1));
        return i;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SAVE_CODE:
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        return;
                    }
                    isEdit = true;
                    updateDisplayPanel(mCurFreq, true);
                    int id = data.getIntExtra(FMSaveChannel.EXTRA_PRESET_ID, 0);
                    updatePresetSwitcher(id + 1);
                    setSelectedPreset(id);
                }
                break;
            case EDIT_CODE:
                if (resultCode == RESULT_OK) {
                    isEdit = true;
                }
                break;
            case CLEAR_CODE:
                if (resultCode == RESULT_OK) {
                    boolean clearedAll = data.getBooleanExtra(FMClearChannel.EXTRA_CLEARED_ALL, false);
                    if (clearedAll) {
                        Log.d(TAG, "Cleared all FM stations");
                        Preferences.setScanned(this, false);
                        setSelectedPreset(-1);
                        isEdit = false;
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_seekbackward:
                mShortPressButtonType = SHORTPRESS_BUTTON_REDUCE;
                handleSeekMsg(view, 1);
                enableUI(false);
                break;
            case R.id.btn_seekforward:
                mShortPressButtonType = SHORTPRESS_BUTTON_ADD;
                handleSeekMsg(view, 0);
                enableUI(false);
                break;
            case R.id.btn_reduce:
            case R.id.btn_add:
                enableUI(false);
                displayRdsScrollText(false);
                isEdit = false;
                if (view.getId() == R.id.btn_reduce) {
                    mCurFreq -= FMUtil.STEP;
                    if (mCurFreq < FMUtil.MIN_FREQUENCY) {
                        mCurFreq = FMUtil.MAX_FREQUENCY;
                    }
                } else {
                    mCurFreq += FMUtil.STEP;
                    if (mCurFreq > FMUtil.MAX_FREQUENCY) {
                        mCurFreq = FMUtil.MIN_FREQUENCY;
                    }
                }
                setFMRadioFrequency();
                updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged() called");
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.main);
        initResourceRefs();
        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
        enableUI(true);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int pos = (int) info.id;
        ContentValues cv;
        Log.d(TAG, "onContextItemSelected, info.id = " + pos);

        switch (item.getItemId()) {
            case PLAY_MENU_ID:
                playClickPreset(pos);
                break;
            case EDIT_MENU_ID:
                editChannel(pos);
                break;
            case REPLACE_MENU_ID:
                boolean hasRds = !TextUtils.isEmpty(mRdsStationName);
                saveStationToDB(pos, mCurFreq, hasRds ? null : "", hasRds ? mRdsStationName : "");
                updateDisplayPanel(mCurFreq, true);
                updatePresetSwitcher();
                setSelectedPreset(pos);
                isEdit = true;
                break;
            case CLEAR_MENU_ID:
                saveStationToDB(pos, 0, "", "");
                updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                break;
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "**************FMRadioMain Activity onCreate() called!****************");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_FM);

        mAM = (AudioManager) getSystemService(AUDIO_SERVICE);
        mCurFreq = Preferences.getLastFrequency(this);

        initResourceRefs();
        mIsBound = bindToService();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(6, getClass().getName());
        mWakeLock.setReferenceCounted(false);
        updateDisplayPanel(mCurFreq, updatePresetSwitcher());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                pDialog = new ProgressDialog(this);
                pDialog.setTitle(getString(R.string.fmradio_scanning_title));
                pDialog.setMessage(getString(R.string.fmradio_scan_begin_msg));
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(true);
                isScanCanceled = false;
                pDialog.setButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        pDialog.cancel();
                    }
                });
                pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        boolean result = false;
                        Log.d("FMRadio Progress_Dialog", "call onCancel");
                        try {
                            result = mService.stopScan();
                        } catch (RemoteException e) {
                            Log.e(TAG, "stopScan Failed: " + e.getMessage());
                        }
                        if (result) {
                            pDialog.dismiss();
                            isScanAll = false;
                            isScanCanceled = true;
                            ignoreRdsEvent(false);
                            if (lastIcon != null) {
                                lastIcon.setVisibility(View.INVISIBLE);
                            }
                            setSelectedPreset(-1);
                            enableUI(true);
                        } else {
                            Log.e(TAG, "stopScan Failed so do not dismiss or update UI. Scan continuing");
                        }
                    }
                });
                return pDialog;
            case DIALOG_SCAN_FINISH:
                return new AlertDialog.Builder(this)
                        .setTitle("Scan Completed")
                        .setMessage("Scaned and Saved " + count_save + " channels")
                        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();
            case DIALOG_SCAN_CANCEL:
                return new AlertDialog.Builder(this)
                        .setTitle("Scan Canceled")
                        .setMessage("Scaned and Saved " + count_save + " channels")
                        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();
            case DIALOG_IF_SCAN_FIRST:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.scan)
                        .setMessage(R.string.fmradio_scan_confirm_msg)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Preferences.setScanned(FMRadioMain.this, true);
                                mCurFreq = FMUtil.MIN_FREQUENCY;
                                ignoreRdsEvent(true);
                                mWakeLock.acquire(LIGHT_ON_TIME);
                                scanFMRadioStation();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                isScanAll = false;
                                ignoreRdsEvent(false);
                            }
                        })
                        .create();
            case DIALOG_IF_SCAN_NEXT:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.scan)
                        .setMessage(R.string.fmradio_clear_confirm_msg)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                clearDB();
                                mCurFreq = FMUtil.MIN_FREQUENCY;
                                ignoreRdsEvent(true);
                                mWakeLock.acquire(LIGHT_ON_TIME);
                                scanFMRadioStation();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ignoreRdsEvent(false);
                            }
                        })
                        .create();
        }

        return null;
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        super.onDestroy();

        resetTimer();
        mHandler.removeMessages(SEEK_NEXT);
        ignoreRdsEvent(false);
        unbindFMRadioService();
        mService = null;
        mWakeLock.release();
        if (pDialog_waitpoweron != null) {
            pDialog_waitpoweron.dismiss();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.btn_seekbackward:
                Log.d(TAG, "Start long press seek backward.");
                mbIsLongPressed = true;
                mLongPressButtonType = LONGPRESS_BUTTON_REDUCE;
                ignoreRdsEvent(true);
                handleSeekMsg(v, 1);
                return true;
            case R.id.btn_reduce:
                enableActiveButton(v);
                displayRdsScrollText(false);
                mbIsLongPressed = true;
                mCurFreq -= FMUtil.STEP;
                if (mCurFreq < FMUtil.MIN_FREQUENCY) {
                    mCurFreq = FMUtil.MAX_FREQUENCY;
                }
                updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                mTimer = new Timer();
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(FM_FREQ_REDUCE);
                    }
                };
                mTimer.schedule(mTimerTask, LONG_PRESS_TUNE_TIMEOUT);
                return true;
            case R.id.btn_add:
                enableActiveButton(v);
                displayRdsScrollText(false);
                mbIsLongPressed = true;
                mCurFreq += FMUtil.STEP;
                if (mCurFreq > FMUtil.MAX_FREQUENCY) {
                    mCurFreq = FMUtil.MIN_FREQUENCY;
                }
                updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                mTimer = new Timer();
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(FM_FREQ_ADD);
                    }
                };
                mTimer.schedule(mTimerTask, LONG_PRESS_TUNE_TIMEOUT);
                return true;
            case R.id.btn_seekforward:
                Log.d(TAG, "Start long press seek forward.");
                mbIsLongPressed = true;
                ignoreRdsEvent(true);
                mLongPressButtonType = LONGPRESS_BUTTON_ADD;
                handleSeekMsg(v, 0);
                return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mService == null) {
            Log.e(TAG, "The connection between FMRadioUI and service had been disconnected.");
            return false;
        }
        switch (item.getItemId()) {
            case SAVE_ID:
                saveChannel(getIndexOfEmptyItem());
                break;
            case EDIT_ID:
                editChannel(getSelectedPreset());
                break;
            case CLEAR_ID:
                Intent clearIntent = new Intent(this, FMClearChannel.class);
                startActivityForResult(clearIntent, CLEAR_CODE);
                break;
            case EXIT_ID:
                Log.d(TAG, "User click Exit Menu to exit FM");
                Preferences.setEnabled(this, false);
                finish();
                break;
            case SCAN_SAVE_ID:
                if (isDBEmpty()) {
                    mCurFreq = FMUtil.MIN_FREQUENCY;
                    mWakeLock.acquire(LIGHT_ON_TIME);
                    scanFMRadioStation();
                } else {
                    showDialog(DIALOG_IF_SCAN_NEXT);
                }
                break;
            case BY_LOUDSPEAKER_ID:
                Log.d(TAG, "setRouting done in java FMRadioPlayer service!");
                try {
                    mService.setAudioRouting(FMRadioPlayerService.AudioManager_ROUTE_FM_SPEAKER);
                } catch (RemoteException e) {
                    Log.e(TAG, "set Audio Routing failed");
                }
                break;
            case BY_HEADSET_ID:
                Log.d(TAG, "setRouting done in java FMRadioPlayer service!");
                try {
                    mService.setAudioRouting(FMRadioPlayerService.AudioManager_ROUTE_FM_HEADSET);
                } catch (RemoteException e) {
                    Log.e(TAG, "set Audio Routing failed");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() called");
        super.onPause();

        ignoreRdsEvent(false);

        Preferences.setLastFrequency(this, mCurFreq);
        Preferences.setLastChannel(this, getSelectedPreset());
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case 0:
                isScanCanceled = false;
                if (pDialog != null) {
                    pDialog.setMessage(getString(R.string.fmradio_scan_begin_msg));
                }
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mService == null) {
            Log.e(TAG, "The connection between FMRadioUI and service had been disconnected.");
            return false;
        }
        super.onPrepareOptionsMenu(menu);

        boolean fmRadioEnabled = Preferences.isEnabled(this);

        menu.clear();
        menu.add(Menu.NONE, CLEAR_ID, Menu.FIRST + 1, R.string.clear_presets).setIcon(R.drawable.ic_menu_clear_channel);
        menu.add(Menu.NONE, EXIT_ID, Menu.FIRST + 4, R.string.exit).setIcon(R.drawable.ic_menu_exit);
        if (isEdit && fmRadioEnabled) {
            menu.add(Menu.NONE, EDIT_ID, Menu.FIRST, R.string.edit_preset).setIcon(R.drawable.ic_menu_edit_preset);
        } else if (!isEdit) {
            menu.add(Menu.NONE, SAVE_ID, Menu.FIRST, R.string.save_preset).setIcon(R.drawable.ic_menu_save_channel);
        }
        if (fmRadioEnabled) {
            menu.add(Menu.NONE, SCAN_SAVE_ID, Menu.FIRST + 3, R.string.scan).setIcon(R.drawable.ic_menu_save_channel);
        }

        int audioRouting = 0;
        try {
            audioRouting = mService.getAudioRouting();
        } catch (RemoteException e) {
            Log.e(TAG, "getAudioRouting failed");
        }
        if (audioRouting == FMRadioPlayerService.AudioManager_ROUTE_FM_HEADSET) {
            menu.add(Menu.NONE, BY_LOUDSPEAKER_ID, Menu.FIRST + 2, R.string.by_loudspeaker).setIcon(R.drawable.ic_menu_loud_speaker);
        } else if (audioRouting == FMRadioPlayerService.AudioManager_ROUTE_FM_SPEAKER) {
            menu.add(Menu.NONE, BY_HEADSET_ID, Menu.FIRST + 2, R.string.by_headset).setIcon(R.drawable.ic_menu_header);
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        Log.d(TAG, "progress is " + progress);
        Log.d(TAG, "Current Frequency is " + mCurFreq);
        Log.d(TAG, "progress fromTouch " + fromTouch);

        if (fromTouch) {
            int step = (progress / FMUtil.STEP) * FMUtil.STEP;
            Log.d(TAG, "step is " + step);
            mCurFreq = RANGE_START + step;
            Log.d(TAG, "Updated Frequency is " + mCurFreq);
            if (mCurFreq < FMUtil.MIN_FREQUENCY) {
                setProgress(FMUtil.MIN_FREQUENCY - RANGE_START);
                mCurFreq = FMUtil.MIN_FREQUENCY;
            } else if (mCurFreq > FMUtil.MAX_FREQUENCY) {
                setProgress(RANGE);
                mCurFreq = FMUtil.MAX_FREQUENCY;
            }
            updateFrequencyDisplay(mCurFreq, false);
            updateDisplayPanel(mCurFreq, updatePresetSwitcher());
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart() called");
        super.onStart();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() called");
        super.onStop();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mTuneSucceed) {
            setFMRadioFrequency();
            mTuneSucceed = false;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "event:" + event.getAction() + ";v.getId:" + v.getId());
        if (event.getAction() == 1 && mbIsLongPressed) {
            mbIsLongPressed = false;
            mLongPressButtonType = LONGPRESS_BUTTON_NONE;
            enableUI(false);
            if (v.getId() == R.id.btn_add || v.getId() == R.id.btn_reduce) {
                Log.d(TAG, "Release button of frequency add or reduce.");
                resetTimer();
                setFMRadioFrequency();
                return true;
            } else if (v.getId() == R.id.btn_seekbackward || v.getId() == R.id.btn_seekforward) {
                Log.d(TAG, "Release button of seek forward or backward.");
                boolean bSeekAbortStatus = false;
                isSeekStarted = false;
                resetTimer();
                if (mService != null) {
                    try {
                        bSeekAbortStatus = mService.stopSeek();
                    } catch (RemoteException e) {
                        Log.e(TAG, "There is a exception to stop seek.");
                    }
                    if (bSeekAbortStatus) {
                        Log.w(TAG, "Abort seek success.");
                        mCurFreq = mPreFreq;
                        setFMRadioFrequency();
                    } else {
                        Log.e(TAG, "Abort seek failed.");
                    }
                    ignoreRdsEvent(false);
                }
                Log.w(TAG, "Seek was aborted, start stop annimation.");
                updateDisplayPanel(mCurFreq, updatePresetSwitcher());
                ScanStopThread sThread = new ScanStopThread();
                mHandler.postDelayed(sThread, SCAN_STOP_DELAY);
                return true;
            }
        }
        return false;
    }

    private void saveStationToDB(int id, int freq, String name, String rdsName) {
        final Uri uri = Uri.withAppendedPath(Channels.CONTENT_URI, String.valueOf(id));
        ContentValues cv = new ContentValues();

        cv.put(Channels.FREQUENCY, freq);
        if (name != null) {
            cv.put(Channels.NAME, name);
        }
        if (rdsName != null) {
            cv.put(Channels.RDS_NAME, rdsName);
        }

        getContentResolver().update(uri, cv, null, null);
    }

    private void setSelectedPreset(int preset) {
        Log.d(TAG, "Select preset " + preset);
        mChannelList.setSelection(preset);
        mChannelList.setItemChecked(preset, true);
    }

    private int getSelectedPreset() {
        int checked = mChannelList.getCheckedItemPosition();
        if (checked == ListView.INVALID_POSITION) {
            return -1;
        }
        return (int) mChannelList.getItemIdAtPosition(checked);
    }

    private void saveChannel(int position) {
        Intent saveIntent = new Intent(this, FMSaveChannel.class);
        saveIntent.putExtra(FMSaveChannel.EXTRA_FREQUENCY, mCurFreq);
        saveIntent.putExtra(FMSaveChannel.EXTRA_PRESET_ID, position);
        saveIntent.putExtra(FMSaveChannel.EXTRA_RDS_NAME, mRdsStationName);
        startActivityForResult(saveIntent, SAVE_CODE);
    }

    private void editChannel(int position) {
        Intent editIntent = new Intent(this, FMEditChannel.class);
        editIntent.putExtra(FMEditChannel.EXTRA_PRESET, position);
        startActivityForResult(editIntent, EDIT_CODE);
    }
}
