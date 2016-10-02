package will.tesler.mousemover;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MouseActivity extends Activity implements SensorEventListener, MouseService.Listener {

    private MouseService.MouseBinder mMouseBinder;
    private ServiceConnection mServiceConnection =  new MouseServiceConnection();
    private SensorManager mSensorManager;

    @BindView(R.id.button_calibrate) Button mButtonCalibrate;
    @BindView(R.id.button_left_click) Button mButtonLeftClick;
    @BindView(R.id.button_right_click) Button mButtonRightClick;

    private boolean mLeftButtonPressed;
    private boolean mRightButtonPressed;

    SharedPreferences mPreferences;
    private Intent mServiceIntent;

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
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //values are angular speed in order x, y, z
        int x = (int) (event.values[2] * 20);
        int y = (int) (event.values[0] * 20);

        if (mMouseBinder != null) {
            mMouseBinder.sendMouseEvent(x, y);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onConnectionError() {
        unbindService(mServiceConnection);
        mMouseBinder = null;
        showConnectionErrorDialog();
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
