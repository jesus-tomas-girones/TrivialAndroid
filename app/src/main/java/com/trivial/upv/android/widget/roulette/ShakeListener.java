package com.trivial.upv.android.widget.roulette;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

public class ShakeListener implements SensorEventListener {
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 5;
    private SensorManager mSensorMgr;
    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;
    private float[] speeds;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float values[] = sensorEvent.values;

        long now = System.currentTimeMillis();
        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }
        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(values[SensorManager.DATA_X] +
                    values[SensorManager.DATA_Y] + values[SensorManager.DATA_Z] -
                    mLastX - mLastY - mLastZ) / diff * 10000;
//            Log.d("SPEED", speed + " " +  mShakeCount);
            if (speed > FORCE_THRESHOLD) {
                if (speeds == null)
                    speeds = new float[SHAKE_COUNT];


                speeds[mShakeCount%SHAKE_COUNT] = speed;
                if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake >
                        SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;

                    float mediaSpeed = 0.0f;
                    for (int j = 0; j < SHAKE_COUNT; j++) {
                        mediaSpeed += speeds[j];
                    }
                    mediaSpeed = mediaSpeed / SHAKE_COUNT;
//                    Log.d("SPEED", "SHAKE");
                    if (mShakeListener != null) {
                        mShakeListener.onShake(mediaSpeed / FORCE_THRESHOLD);
                    }
                }
                mLastForce = now;

            }
            mLastTime = now;
            mLastX = values[SensorManager.DATA_X];
            mLastY = values[SensorManager.DATA_Y];
            mLastZ = values[SensorManager.DATA_Z];
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public interface OnShakeListener {
        public void onShake(float speed);
    }

    public ShakeListener(Context context) {
        mContext = context;
        resume();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        mShakeListener = listener;
    }

    private Sensor sensor;

    public void resume() {
        mSensorMgr = (SensorManager) mContext.getSystemService(Context
                .SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensores no soportados");
        }

        List<Sensor> listaSensores = mSensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (!listaSensores.isEmpty()) {
            sensor = listaSensores.get(0);
            mSensorMgr.registerListener(this, sensor, SENSOR_DELAY_GAME);
        }
    }

    public void pause() {
        if (mSensorMgr != null && sensor != null) {
            mSensorMgr.unregisterListener(this, sensor);
            mSensorMgr = null;
        }
    }
}