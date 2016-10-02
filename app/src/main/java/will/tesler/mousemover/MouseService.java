package will.tesler.mousemover;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MouseService extends Service {

    public static final String EXTRA_SERVER_IP = "will.tesler.mousemover.SERVER_IP";
    public static final String EXTRA_PORT = "will.tesler.mousemover.PORT";

    private static final int CODE_CALIBRATE = Integer.MAX_VALUE;
    private static final int CODE_LEFT_CLICK_DOWN = Integer.MIN_VALUE;
    private static final int CODE_LEFT_CLICK_UP = Integer.MIN_VALUE + 1;
    private static final int CODE_RIGHT_CLICK_DOWN = Integer.MIN_VALUE + 2;
    private static final int CODE_RIGHT_CLICK_UP = Integer.MIN_VALUE + 3;

    Socket mSocket;
    DataOutputStream mDataOutputStream;

    MouseBinder mMouseBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MouseService", "Service Created.");
        mMouseBinder = new MouseBinder();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        Log.d("MouseService", "Service Bound.");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String serverIp = intent.getStringExtra(EXTRA_SERVER_IP);
                    int port = intent.getIntExtra(EXTRA_PORT, -1);
                    mSocket = new Socket(serverIp, port);
                    mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();

        return mMouseBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d("MouseService", "Service Destroyed.");
    }

    public class MouseBinder extends Binder {

        public void sendCalibration() {
            sendValues(CODE_CALIBRATE);
        }

        public void sendLeftClickDown() {
            sendValues(CODE_LEFT_CLICK_DOWN);
        }

        public void sendLeftClickUp() {
            sendValues(CODE_LEFT_CLICK_UP);
        }

        public void sendRightClickDown() {
            sendValues(CODE_RIGHT_CLICK_DOWN);
        }

        public void sendRightClickUp() {
            sendValues(CODE_RIGHT_CLICK_UP);
        }

        public void sendMouseEvent(final int x, final int y) {
            sendValues(x, y);
        }

        private void sendValues(final int... values) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mDataOutputStream != null) {
                        try {
                            for (int value : values) {
                                mDataOutputStream.writeInt(value);
                            }
                            mDataOutputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d("MouseService", "Sending mouse event.");
                    } else {
                        Log.w("MouseService", "No output stream available.");
                    }
                }
            };
            new Thread(runnable).start();
        }
    }
}
