package will.tesler.mousemover;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MouseActivity extends Activity implements SensorEventListener, MouseService.Listener {

    private static final int TOUCH_DELAY_MS = 200;
    private static final float LERP_FACTOR = .20f;

    private MouseService.MouseBinder mMouseBinder;
    private ServiceConnection mServiceConnection = new MouseServiceConnection();
    private SensorManager mSensorManager;
    private Vibrator mVibrator;

    @BindView(R.id.button_calibrate) FloatingActionButton mButtonCalibrate;
    @BindView(R.id.imagebutton_keyboard) ImageButton mButtonKeyboard;

    private PressState mLeftPressState = new PressState();
    private PressState mRightPressState = new PressState();

    SharedPreferences mPreferences;
    private Intent mServiceIntent;

    private float mLastX = Integer.MIN_VALUE;
    private float mLastY = Integer.MIN_VALUE;

    private Handler mLeftPressHandler;
    private Handler mRightPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedPairingCode = preferences.getString(PreferenceConstants.PAIRING_CODE, "");

        mServiceIntent = new Intent(this, MouseService.class)
                .putExtra(MouseService.EXTRA_SERVER_IP, savedPairingCode)
                .putExtra(MouseService.EXTRA_PORT, 63288);
        bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mLeftPressHandler = new Handler();
        mRightPressHandler = new Handler();
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

        Debug.I("Binding to Service.");

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
            Debug.I("Unbinding from Service.");
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
        long[] pattern = new long[]{0, 100, 50, 50, 50, 25};
        mVibrator.vibrate(pattern, -1);
        Debug.I("Calibrate Pressed");
    }

    @OnClick(R.id.imagebutton_keyboard)
    void onKeyboardButtonClick(View v) {
        KeyboardUtils.showSoftKeyboard(v);
        mVibrator.vibrate(25);
        Debug.I("Keyboard Pressed");
    }

    @OnClick(R.id.button_left_click)
    void onLeftButtonClick() {
        if (mMouseBinder != null) {
            mMouseBinder.sendLeftClick();
        }
        mVibrator.vibrate(25);
        Debug.I("Left Click");
    }

    @OnClick(R.id.button_right_click)
    void onRightButtonClick() {
        if (mMouseBinder != null) {
            mMouseBinder.sendRightClick();
        }
        mVibrator.vibrate(25);
        Debug.I("Right Click");
    }

    @OnTouch(R.id.button_left_click)
    boolean onLeftButtonTouch(MotionEvent event) {
        return onButtonTouch(event, true, mLeftPressState, mLeftPressHandler);
    }

    @OnTouch(R.id.button_right_click)
    boolean onRightButtonTouch(MotionEvent event) {
        return onButtonTouch(event, false, mRightPressState, mRightPressHandler);
    }

    private boolean onButtonTouch(final MotionEvent event, final boolean isLeftButton,
                                  final PressState pressState, final Handler pressHandler) {
        final String side = isLeftButton ? "Left" : "Right";
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!pressState.isPressed) {
                    pressHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pressState.isPressed = true;
                            if (mMouseBinder != null) {
                                if (isLeftButton) {
                                    mMouseBinder.sendLeftDown();
                                } else {
                                    mMouseBinder.sendRightDown();
                                }
                            }
                            mVibrator.vibrate(25);
                            Debug.I(side + " Down");
                        }
                    }, TOUCH_DELAY_MS);
                }
                break;
            case MotionEvent.ACTION_UP:
                pressHandler.removeCallbacksAndMessages(null);
                if (pressState.isPressed) {
                    pressState.isPressed = false;
                    if (mMouseBinder != null) {
                        if (isLeftButton) {
                            mMouseBinder.sendLeftUp();
                        } else {
                            mMouseBinder.sendRightUp();
                        }
                    }
                    mVibrator.vibrate(25);
                    Debug.I(side + " Up");
                    return true;
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
            x = lerp(mLastX, x);
            y = lerp(mLastY, y);
        }

        mLastX = x;
        mLastY = y;

        if (mMouseBinder != null) {
            mMouseBinder.sendMousePosition((int) x, (int) y);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Debug.I("Accuracy changed to: " + accuracy);
    }

    @Override
    public void onConnectionError() {
        try {
            unbindService(mServiceConnection);
            mMouseBinder = null;
        } catch (IllegalStateException e) {
            Debug.W("Unbind attempted while unbound.");
        }
        Debug.W("Connection Error.");
        showConnectionErrorDialog();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Debug.I("Back Pressed.");
            super.onBackPressed();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            KeyboardUtils.hideSoftKeyboard(mButtonKeyboard);
        }

        if (mMouseBinder != null) {
            int pressedKey = event.getUnicodeChar();
            System.out.println("Sending unicode character: " + pressedKey);
            mMouseBinder.sendUnicodeChar(pressedKey);
        }
        Debug.I("Key Pressed: " + event.getUnicodeChar());
        return true;
    }


    private void showConnectionErrorDialog() {
        AlertDialog.Builder builder = DialogUtils.createConnectionErrorDialog(this, mPreferences)
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
                });
        builder.create().show();
    }

    private float lerp(float a, float b) {
        return (a * (1.0f - LERP_FACTOR)) + (b * LERP_FACTOR);
    }

    private class MouseServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Debug.I("Connected to service");
            mMouseBinder = (MouseService.MouseBinder) service;
            mMouseBinder.setListener(MouseActivity.this);
            Debug.I("Contacting server.");
            mMouseBinder.contactServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Debug.I("Disconnected from service");
            mMouseBinder = null;
        }
    }

    private class PressState {
        boolean isPressed;
    }
}
