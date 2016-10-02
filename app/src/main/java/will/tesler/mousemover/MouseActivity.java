package will.tesler.mousemover;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MouseActivity extends Activity implements SensorEventListener, MouseService.Listener {

    private MouseService.MouseBinder mMouseBinder;
    private ServiceConnection mServiceConnection =  new MouseServiceConnection();
    private SensorManager mSensorManager;

    @BindView(R.id.button_calibrate) FloatingActionButton mButtonCalibrate;
    @BindView(R.id.button_left_click) Button mButtonLeftClick;
    @BindView(R.id.button_right_click) Button mButtonRightClick;
    @BindView(R.id.imagebutton_keyboard) ImageButton mImageButtonKeyboard;

    private boolean mLeftButtonPressed;
    private boolean mRightButtonPressed;

    SharedPreferences mPreferences;
    private Intent mServiceIntent;

    private float mLastX = Integer.MIN_VALUE;
    private float mLastY = Integer.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedPairingCode = preferences.getString(PreferenceConstants.PAIRING_CODE, "");

        mServiceIntent = new Intent(this, MouseService.class)
            .putExtra(MouseService.EXTRA_SERVER_IP, savedPairingCode)
            .putExtra(MouseService.EXTRA_PORT, 63288);
        bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String savedPairingCode = mPreferences.getString(PreferenceConstants.PAIRING_CODE, "");

        Intent intent = new Intent(this, MouseService.class);
        intent.putExtra(MouseService.EXTRA_SERVER_IP, savedPairingCode);
        intent.putExtra(MouseService.EXTRA_PORT, 63288);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        List<Sensor> typedSensors = mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
        if (typedSensors != null && typedSensors.size() > 0) {
            mSensorManager.registerListener(this, typedSensors.get(0), SensorManager.SENSOR_DELAY_FASTEST);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);

        if (mMouseBinder != null) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.button_calibrate)
    void onCalibrateButtonClick() {
        if (mMouseBinder != null) {
            mMouseBinder.sendCalibration();
        }
    }

    @OnClick(R.id.imagebutton_keyboard)
    void onKeyboardButtonClick(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }

    @OnTouch(R.id.button_left_click)
    boolean onLeftButtonTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mLeftButtonPressed) {
                    mLeftButtonPressed = true;
                    if (mMouseBinder != null) {
                        mMouseBinder.sendLeftClickDown();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mLeftButtonPressed) {
                    mLeftButtonPressed = false;
                    if (mMouseBinder != null) {
                        mMouseBinder.sendLeftClickUp();
                    }
                }
                break;
        }
        return false;
    }

    @OnTouch(R.id.button_right_click)
    boolean onRightButtonTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mRightButtonPressed) {
                    mRightButtonPressed = true;
                    if (mMouseBinder != null) {
                        mMouseBinder.sendRightClickDown();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mRightButtonPressed) {
                    mRightButtonPressed = false;
                    if (mMouseBinder != null) {
                        mMouseBinder.sendRightClickUp();
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //values are angular speed in order x, y, z
        float x = event.values[2] * 20;
        float y = event.values[0] * 20;
        if (mLastX != Integer.MIN_VALUE) {
            x = lerp(mLastX, x, .5f);
            y = lerp(mLastY, y, .5f);
        }

        if (mMouseBinder != null) {
            mMouseBinder.sendMouseEvent((int) x,(int) y);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onConnectionError() {
        if (mMouseBinder != null) {
            unbindService(mServiceConnection);
            mMouseBinder = null;
        }
        showConnectionErrorDialog();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
        }

        if (mMouseBinder != null) {
            int pressedKey = event.getUnicodeChar();
            System.out.println("Sending unicode character: " + pressedKey);
            mMouseBinder.sendUnicodeChar(pressedKey);
        }
        return true;
    }


    private void showConnectionErrorDialog() {
        String savedPairingCode = mPreferences.getString(PreferenceConstants.PAIRING_CODE, "");

        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cannot connect to device")
                .setMessage("Ensure MouseMover is enabled on the other device and the pairing code is " +
                        savedPairingCode)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Main Menu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private float lerp(float a, float b, float factor) {
        return (a * (1.0f - factor)) + (b * factor);
    }

    private class MouseServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMouseBinder = (MouseService.MouseBinder) service;
            mMouseBinder.setListener(MouseActivity.this);
            mMouseBinder.contactServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMouseBinder = null;
        }
    }
}
