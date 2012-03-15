/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cordova.compass;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.apache.cordova.json4j.JSONArray;
import org.apache.cordova.json4j.JSONException;
import org.apache.cordova.json4j.JSONObject;
import org.apache.cordova.util.Logger;
import java.lang.IllegalStateException;

import net.rim.device.api.system.MagnetometerData;
import net.rim.device.api.system.MagnetometerListener;
import net.rim.device.api.system.MagnetometerSensor;
import net.rim.device.api.system.MagnetometerSensor.Channel;
import net.rim.device.api.system.MagnetometerChannelConfig;
import net.rim.device.api.system.MagnetometerCalibrationException;
import net.rim.device.api.system.Application;
import net.rim.device.api.ui.UiApplication;

public class Compass extends Plugin implements MagnetometerListener{
    public static final String ACTION_GET_HEADING = "getHeading";
    public static final String ACTION_STOP = "stop";
    
    public static int STOPPED = 0;
    public static int STARTED = 1;
    
    private Channel magChannel;
    int status;
    public float timeout = 30000;
    long lastAccessTime;
    
    public PluginResult execute(String action, JSONArray args, String calbackId) {
    
		PluginResult result = null;
		if (!MagnetometerSensor.isSupported()) {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "Magnetometer sensor not supported");
		}
		else if (ACTION_GET_HEADING.equals(action)) {
			JSONObject heading = new JSONObject();
           
			try {
                MagnetometerData data = getCurrentHeading();
				heading.put("magneticHeading", data.getDirectionTop());
				heading.put("trueHeading", data.getDirectionTop());
				heading.put("headingAccuracy", 0);
				heading.put("timestamp", data.getTimestamp());
			} catch (JSONException e) {
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION, "JSONException:" + e.getMessage());
			}
            
			result = new PluginResult(PluginResult.Status.OK, heading);
		}
		else if (ACTION_STOP.equals(action)) {
			this.stop();
			return new PluginResult(PluginResult.Status.OK, "");
		}
		else {
			result = new PluginResult(PluginResult.Status.INVALID_ACTION, "Magnetometer: Invalid action:" + action);
		}

		return result;    
    }

    /**
     * Get status of magnetometer sensor.
     *
     * @return			status
     */
   
	public int getStatus() {
		return this.status;
	}

	/**
	 * Set the status and send it to JavaScript.
	 * @param status
	 */

	private void setStatus(int status) {
		this.status = status;
	}
    
    /**
	 * Get the latest data from the Magnetometer sensor
	 *
	 * @return the MagnetometerData 
	 */
    private MagnetometerData getCurrentHeading(){ 
		// open sensor channel
		if (this.getStatus() != STARTED) {
			this.start();
		}

		// get the last acceleration
		MagnetometerData data = magChannel.getData();

		// remember the access time (for timeout purposes)
        this.lastAccessTime = System.currentTimeMillis();

		return data;
    }
    
	/**
	 * Implements the MagnetometerListener method.  We listen for the purpose
	 * of closing the application's Magnetometer sensor channel after timeout
	 * has been exceeded.
	 */
    
	public void onData(MagnetometerData magData) {
        long timestamp = magData.getTimestamp();

        // If values haven't been read for length of timeout,
        // turn off magnetometer sensor to save power
		if ((timestamp - this.lastAccessTime) > this.timeout) {
			magChannel.close();
		}
	}
    
    /**
	 * Starts the Magnetometer channel to get data. Set to background mode to get the raw data
	 */
	public void start(){
        MagnetometerChannelConfig channelConfig = new MagnetometerChannelConfig();
        channelConfig.setBackgroundMode(true);
        magChannel = MagnetometerSensor.openChannel(Application.getApplication(),channelConfig);
        magChannel.addMagnetometerListener(this);

        // can't figure this out at the moment
        /*
        Thread t = new Thread(new Runnable() {
            public void run() {
                try{
                    magChannel.startCalibration();
                }catch(MagnetometerCalibrationException e){
                    Logger.log("MagnetometerCalibrationException:" + e.getMessage());
                } catch(IllegalStateException e){
                    Logger.log("IllegalStateException:" + e.getMessage());
                }
            }
        });
        t.start(); 
        */
        
		Logger.log(this.getClass().getName() +": sensor listener added");

		this.setStatus(STARTED);
	}

    /**
     * Stops magnetometer listener and closes the sensor channel.
     */
    public void stop() {
        // close the sensor channel
        if (magChannel != null && magChannel.isOpen()) {
            magChannel.close();
            Logger.log(this.getClass().getName() +": sensor channel closed");
        }

        this.setStatus(STOPPED);
    }

    /**
     * Called when Plugin is destroyed.
     */
     
    public void onDestroy() {
        magChannel.close();
    }
}

