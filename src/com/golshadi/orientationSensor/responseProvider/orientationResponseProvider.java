package com.golshadi.orientationSensor.responseProvider;

import com.golshadi.orientationSensor.utils.AppConstants;
import com.golshadi.orientationSensor.utils.OrientationSensorInterface;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Majid Golshadi
 * Date: 2/10/14
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class orientationResponseProvider {

    private ArrayList<Double> sensorValueLog = new ArrayList<Double>();
    private ArrayList<Double> tolerance = new ArrayList<Double>();
    private JSONObject response = new JSONObject();
    private OrientationSensorInterface observer;

    public orientationResponseProvider(OrientationSensorInterface osi){
        sensorValueLog.add(0, 0.0);
        sensorValueLog.add(1, 0.0);
        sensorValueLog.add(2, 0.0);

        tolerance.add(0, 0.0);
        tolerance.add(1, 0.0);
        tolerance.add(2, 0.0);

        this.observer = osi;
    }

    public void init(Double azimtuhTol, Double pitchTol, Double rollTol){
        tolerance.add(0, azimtuhTol);
        tolerance.add(1, pitchTol);
        tolerance.add(2, rollTol);
    }

    public void dispatcher(float[] gyroOrientation){

        Double azimuth = gyroOrientation[0] * 180/Math.PI;
        if ( azimuth < 0)
        	azimuth += 360;
        Double pitch = gyroOrientation[1] * 180/Math.PI;
        Double roll = gyroOrientation[2] * 180/Math.PI;

        if ( Math.abs(sensorValueLog.get(0) - azimuth) > tolerance.get(0)
                || Math.abs(sensorValueLog.get(1) - pitch) > tolerance.get(1)
                || Math.abs(sensorValueLog.get(2) - roll) > tolerance.get(2))
        {
            sensorValueLog.set(0, azimuth);
            sensorValueLog.set(1, pitch);
            sensorValueLog.set(2, roll);

            observer.orientation(sensorValueLog.get(0), sensorValueLog.get(1), sensorValueLog.get(2));
        }
    }

}
