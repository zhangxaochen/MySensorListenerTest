package com.example.mysensorlistener;

import java.util.LinkedList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MySensorListener implements SensorEventListener {
	
	//用于标记并去除第一帧，因为第一帧数据总是上一次unregister之前的遗留帧，原因不明
	private boolean _isFirstFrame=true;
	
	//2013-6-26 23:39:44	试图时间戳对齐，接姜锦正要求
	private int INVALID=-1;
	private long _timeStamp=INVALID;
	private final int _sensorNum=4;
	private int _sensorCnt=0;
	
	private float[] _tmpAcc;
	private float[] _tmpGyro;
	private float[] _tmpMag;
	private float[] _tmpRot;
	
	
	/**
	 * _aBuffer 是合加速度， _laBuffer 是线加速度
	 */
	private LinkedList<float[]> _aBuffer = new LinkedList<float[]>();
	/**
	 * linear acceleration
	 */
	private LinkedList<float[]> _laBuffer = new LinkedList<float[]>();
	/**
	 * gravity
	 */
	private LinkedList<float[]> _gBuffer = new LinkedList<float[]>();
	/**
	 * megnetic field
	 */
	private LinkedList<float[]> _mBuffer = new LinkedList<float[]>();
	/**
	 * gyroscope
	 */
	private LinkedList<float[]> _gyroBuffer = new LinkedList<float[]>();

	/**
	 * rotation vector
	 */
	private LinkedList<float[]> _rotBuffer = new LinkedList<float[]>();
	
	private LinkedList<Long> _tsBuffer=new LinkedList<Long>();

	public class MySensorData {
		/**
		 * _abuf 是合加速度
		 */
		LinkedList<float[]> _abuf;
		/**
		 * it's gyroscope, not gravity
		 */
		LinkedList<float[]> _gbuf;
		/**
		 * magnetic field
		 */
		LinkedList<float[]> _mbuf;
		/**
		 * it's rotation vector, not rotation
		 */
		LinkedList<float[]> _rbuf;
		
		/**
		 * timeStamp
		 */
		LinkedList<Long> _tsBuf;

		public MySensorData() {
		}

		public LinkedList<float[]> getAbuf() {
			return _abuf;
		}

		public LinkedList<float[]> getGbuf() {
			return _gbuf;
		}

		public LinkedList<float[]> getMbuf() {
			return _mbuf;
		}

		public LinkedList<float[]> getRbuf() {
			return _rbuf;
		}
		public LinkedList<Long> getTbuf(){
			return _tsBuf;
		}

		public void clearAllBuf() {
			MySensorListener.this.clearAllBuf();
		}

	}

	private MySensorData _sensorData = new MySensorData();

	public MySensorListener() {
		_sensorData._abuf = _aBuffer;
		_sensorData._gbuf = _gyroBuffer;
		_sensorData._mbuf = _mBuffer;
		_sensorData._rbuf = _rotBuffer;
		
		_sensorData._tsBuf=_tsBuffer;
	}
	
	private void offerBuffers(){
		System.out.println("offerBuffers()");
		if (_isFirstFrame) {
			System.out.println("if(_isFirstFrame)");
			_isFirstFrame=false;
			return;
		}
//		_sensorCnt=0;
		Long ts=_timeStamp/(1000*1000);
		if(ts.equals(_tsBuffer.peekLast()))
			return;
		//old timestamp, in millis:
		_tsBuffer.offer(ts);

		_aBuffer.offer(_tmpAcc);
		_gyroBuffer.offer(_tmpGyro);
		_mBuffer.offer(_tmpMag);
		_rotBuffer.offer(_tmpRot);
	}
	private void addValidValues(int eType, float[] values){
		_sensorCnt++;
//		if (eType == Sensor.TYPE_ACCELEROMETER) {
//			_tmpAcc=values;
//		} else if (eType == Sensor.TYPE_MAGNETIC_FIELD) {
//			_tmpMag=values;
//		} else if (eType == Sensor.TYPE_GYROSCOPE) {
//			_tmpGyro=values;
//		} else if (eType == Sensor.TYPE_ROTATION_VECTOR) {
//			_tmpRot=values;
//		}else{
//			_sensorCnt--;
//		}
		switch (eType) {
		case Sensor.TYPE_ACCELEROMETER:
			_tmpAcc=values;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			_tmpMag=values;
			break;
		case Sensor.TYPE_GYROSCOPE:
			_tmpGyro=values;
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			_tmpRot=values;
			break;
		default:
			_sensorCnt--;
			break;
		}

		System.out.println("addValidValues, _sensorCnt: "+_sensorCnt);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
//		 System.out.println("onSensorChanged");

		int eType = event.sensor.getType();
		float[] values = event.values.clone();
		
		//伪代码见手机照片
		long ts=event.timestamp;
		System.out.println("System.currentTimeMillis(), e.ts: "+System.currentTimeMillis()+", "+event.timestamp+", "+eType+", "+_timeStamp);
		
		if(_timeStamp==INVALID){
			System.out.println("_timeStamp==INVALID");
			_timeStamp=ts;
		}
		
		if(ts-_timeStamp<0)
			System.out.println("=======================");
		
		if(_timeStamp!=ts){
			System.out.println("_timeStamp!=ts, "+_timeStamp+", "+ts+", "+eType);
			if(_sensorCnt>=_sensorNum){
				System.out.println("_sensorCnt>=_sensorNum");
				offerBuffers();
			}
			else
				System.out.println("_timeStamp!=ts && _sensorCnt<_sensorNum");
			_timeStamp=ts;
			_sensorCnt=0;
		}
		else
			System.out.println("_timeStamp==ts, "+ts);
			
		addValidValues(eType, values);
		

//		if (eType == Sensor.TYPE_ACCELEROMETER) {
//			_aBuffer.offer(values);
//			//加时间戳：
//			 _tsBuffer.offer(System.currentTimeMillis());
//			System.out.println("onSensorChanged values: "+values[0]+","+values[1]+","+values[2]);
//		} else if (eType == Sensor.TYPE_LINEAR_ACCELERATION) {
//			_laBuffer.offer(values);
//		} else if (eType == Sensor.TYPE_GRAVITY) {
//			_gBuffer.offer(values);
//		} else if (eType == Sensor.TYPE_MAGNETIC_FIELD) {
//			_mBuffer.offer(values);
//		} else if (eType == Sensor.TYPE_ORIENTATION) {
//			// do nothing
//		} else if (eType == Sensor.TYPE_GYROSCOPE) {
//			_gyroBuffer.offer(values);
//		} else if (eType == Sensor.TYPE_ROTATION_VECTOR) {
//			_rotBuffer.offer(values);
////			System.out.println("values.length:= "+values.length);	//==3
//		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	public LinkedList<float[]> getAccDataBuf() {
		return _aBuffer;
	}

	public LinkedList<float[]> getGravityDataBuf() {
		return _gBuffer;
	}

	public LinkedList<float[]> getLinearAccDataBuf() {
		return _laBuffer;
	}

	public LinkedList<float[]> getMegDataBuf() {
		return _mBuffer;
	}

	public LinkedList<float[]> getGyroDataBuf() {
		return _gyroBuffer;
	}

	public LinkedList<float[]> getRotDataBuffer() {
		return _rotBuffer;
	}

	public void registerWithSensorManager(SensorManager sm, int rate) {
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GRAVITY),
				rate);
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate);
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), rate);
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate);
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				rate);
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), rate);
		
		
	}

	public void unregisterWithSensorManager(SensorManager sm) {
		sm.unregisterListener(this);
	}

	public MySensorData getSensorData() {
		return _sensorData;
	}

	public void setSensorData(MySensorData _sensorData) {
		this._sensorData = _sensorData;
	}

	public void clearAllBuf() {
		_isFirstFrame=true;
		
		
		_aBuffer.clear();
		_laBuffer.clear();
		_gBuffer.clear();
		_mBuffer.clear();
		_gyroBuffer.clear();
		_rotBuffer.clear();
		
		_tsBuffer.clear();
	}
	
	public void reset(){
		_timeStamp=INVALID;
		_sensorCnt=0;
	}

}// MySensorListener

