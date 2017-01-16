package se.hshn.de.pathtracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

/**
 * Created by florens on 12.01.17.
 */

public class StepEstimator implements SensorEventListener {

    private float[][] sensor_cache = new float[30][3];
    private float[] orientation = new float[3];
    private float[] R = new float[16];


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float[] values = new float[3];

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            float[] tempVal = new float[3];
            tempVal[0] = sensorEvent.values[2];
            tempVal[1] = sensorEvent.values[1];
            tempVal[2] = sensorEvent.values[0];

            SensorManager.getRotationMatrixFromVector(R, tempVal);
            System.arraycopy(SensorManager.getOrientation( R, orientation ), 0, values, 0, 3);
            sensor_cache[Sensor.TYPE_ORIENTATION] = values;

        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensor_cache[sensorEvent.sensor.getType()] = sensorEvent.values;
        }
/*        else if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            float[] res = transformToEarthData(sensorEvent.values, R);
            //System.out.println(res[0]);
            //System.out.println(res[1]);
            //System.out.println(res[2]);

            if (res[0] > 10 || res[1] > 10) {
                double direction = Math.atan2(res[1], res[0]);

                double degree = Math.toDegrees(direction);
                System.out.println(degree);
            }


        }*/

    }

    private float[] transformToEarthData(float[] data, float[] R) {

        float[] deviceRelative = new float[4];
        deviceRelative[0] = data[0];
        deviceRelative[1] = data[1];
        deviceRelative[2] = data[2];
        deviceRelative[3] = 0;

        float[] inv = new float[16], res = new float[16];
        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(res, 0, inv, 0, deviceRelative, 0);

        return res;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    Measurement getStep(Location currentLocation) {
        Measurement m = new Measurement();
        m.setTimestamp(System.currentTimeMillis());

        if (currentLocation.getTime() + 5000 > System.currentTimeMillis()) {
            m.setLon(currentLocation.getLongitude());
            m.setLat(currentLocation.getLatitude());
            m.setAccuracy(currentLocation.getAccuracy());
        }

        m.setMagx(sensor_cache[Sensor.TYPE_MAGNETIC_FIELD][0]);
        m.setMagy(sensor_cache[Sensor.TYPE_MAGNETIC_FIELD][1]);
        m.setMagz(sensor_cache[Sensor.TYPE_MAGNETIC_FIELD][2]);
        m.setLength(0.65f);


        //double direction = Math.atan2(sensor_cache[Sensor.TYPE_ACCELEROMETER][2], sensor_cache[Sensor.TYPE_ACCELEROMETER][1]);
/*
        double direction = Math.atan(sensor_cache[Sensor.TYPE_LINEAR_ACCELERATION][1] / sensor_cache[Sensor.TYPE_LINEAR_ACCELERATION][0]);

        double degree = Math.toDegrees(direction);
        System.out.println(degree);
        m.setDirection((float) direction);
*/

        m.setAzimuth(sensor_cache[Sensor.TYPE_ORIENTATION][0]);
        m.setPitch(sensor_cache[Sensor.TYPE_ORIENTATION][1]);
        m.setRoll(sensor_cache[Sensor.TYPE_ORIENTATION][2]);

        return m;
    }

}
