package com.golshadi.orientationSensor.sensors;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import com.golshadi.orientationSensor.math.Matrix3x3;
import com.golshadi.orientationSensor.responseProvider.orientationResponseProvider;
import com.golshadi.orientationSensor.utils.OrientationSensorInterface;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: Majid Golshadi
 * Date: 2/10/14
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Orientation implements Isensor,Observer {

    private accelerometer accSensro;
    private gyroscope gySensor;
    private magnetic mgSensor;
    private Handler orientationHandler = new Handler();
    private orientationResponseProvider responseProvider;

    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer;

    private float[] rotationMatrix = new float[9];
    private float[] accMagOrientation = new float[3];
    private float[] fusedOrientation = new float[3];

    private float[] accel = new float[3];
    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];
    private float[] magnet = new float[3];

    public Orientation(Context context, OrientationSensorInterface osi){
        accSensro = new accelerometer(context);
        gySensor = new gyroscope(context);
        mgSensor = new magnetic(context);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        responseProvider = new orientationResponseProvider(osi);
    }
    
    public void dispose(){
    	orientationHandler = null;
    	
    	rotationMatrix = null;
        accMagOrientation = null;
        fusedOrientation = null;

        accel = null;
        gyro = null;
        gyroMatrix = null;
        gyroOrientation = null;
        magnet = null;
        
        accSensro = null;
        gySensor = null;
        mgSensor = null;
        
        responseProvider = null;
    }
    
    public void forceDispose(){        
    	dispose();
        System.gc();
    }

    public void init(Double azimuthTol, Double pitchTol, Double rollTol){
        responseProvider.init(azimuthTol, pitchTol, rollTol);
    }

    @Override
    public boolean isSupport() {
        if (accSensro.isSupport() && mgSensor.isSupport())
        	return true;
        
        return false;
    }

    @Override
    public void on(int speed) {

        if (accSensro.isSupport()){
            accSensro.addObserver(this);
            accSensro.on(speed);
        }

        if (gySensor.isSupport()){
            gySensor.addObserver(this);
            gySensor.on(speed);
        }

        if (mgSensor.isSupport()){
            mgSensor.addObserver(this);
            mgSensor.on(speed);
        }

        // time reference: http://webraidmobile.wordpress.com/2010/10/21/how-long-is-sensor_delay_game/
        fuseTimer = new Timer();
        switch (speed){
            case 0:
                fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                        1, 224);
                break;
            case 1:
                fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                        1, 77);
                break;
            case 2:
                fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                        1, 37);
                break;
            default:
                fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                        1, 16);
        }

    }

    @Override
    public void off() {
    	fuseTimer.cancel();
    	
        if (accSensro.isSupport()){
            accSensro.deleteObserver(this);
            accSensro.off();
        }

        if (gySensor.isSupport()){
            gySensor.deleteObserver(this);
            gySensor.off();
        }

        if (mgSensor.isSupport()){
            mgSensor.deleteObserver(this);
            mgSensor.off();
        }
    }

    @Override
    public float getMaximumRange() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(Observable observable, Object o) {

        if (observable instanceof accelerometer){
            System.arraycopy(accSensro.getEvent().values,
                    0, accel, 0, 3);
            calculateAccMagOrientation();
        }

        if (observable instanceof gyroscope)
            gyroFunction(gySensor.getEvent());


        if (observable instanceof magnetic)
            System.arraycopy(mgSensor.getEvent().values, 0, magnet, 0, 3);

    }

    public void updateValues(){
        responseProvider.dispatcher(fusedOrientation);
    }

    public Runnable updateOrientationValueTask = new Runnable() {
        @Override
        public void run() {
            updateValues();
        }
    };

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179� <--> -179� transition problem:
             * Check whether one of the two Orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
             * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and Orientation with fused Orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);


            // update sensor output
            orientationHandler.post(updateOrientationValueTask);
        }
    }

    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based Orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {

        if (accMagOrientation == null)
            return;


        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = Matrix3x3.multiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        timestamp = event.timestamp;


        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        gyroMatrix = Matrix3x3.multiplication(gyroMatrix, deltaMatrix);

        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = Matrix3x3.multiplication(xM, yM);
        resultMatrix = Matrix3x3.multiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private void getRotationVectorFromGyro(float[] gyroValues, float[] deltaRotationVector, float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the sample angular speed
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

}
