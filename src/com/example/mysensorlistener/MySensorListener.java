package com.example.mysensorlistener;

import java.util.LinkedList;

import android.R.integer;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class MySensorListener implements SensorEventListener {
	//2015��3��24��23:43:27 ����ά�� HuaweiProj ʱ��
	//OpenCameraZC �õ�����Ϊ����һ��������Ҫ IMU ����ʾ��λ�ǡ�����ǣ����Ǵ浽 xml �ļ�Ҫ����ť֮�������д�FLAG
	public boolean _allowStoreData = false;
	
	//���ڱ�ǲ�ȥ����һ֡����Ϊ��һ֡����������һ��unregister֮ǰ������֡��ԭ����
	private boolean _isFirstFrame=true;
	
	//ȥ����Ԫ����һ֡�� ������ڶ�֡ʱ��� <1s, �� 0.2s�� �������Ϊ 0.02s�� ʮ��ʱ����ڲ�ֵʱ�����������
	private boolean _aIsFirstFrame=true;
	private boolean _gIsFirstFrame=true;
	private boolean _mIsFirstFrame=true;
	private boolean _rIsFirstFrame=true;
	private boolean _laIsFirstFrame=true;
	
	//2015-4-8 09:50:35
	TelephonyManager _tManager;
	private int _signalStrength = 0;
	private int _cellId = -1;
	
	//2013-6-26 23:39:44	��ͼʱ������룬�ӽ�����Ҫ��
	private int INVALID=-1;
	private long _timeStamp=INVALID;
	private final int _sensorNum=4;
	private int _sensorCnt=0;
	private int _maxBufSize=200000;
	
//	private float[] _tmpAcc;
//	private float[] _tmpGyro;
//	private float[] _tmpMag;
//	private float[] _tmpRot;

	float[] _lastRotVec;
	
	/**
	 * _aBuffer �Ǻϼ��ٶȣ� _laBuffer ���߼��ٶ�
	 */
	private LinkedList<float[]> _aBuffer = new LinkedList<float[]>();
	/**
	 * linear acceleration
	 */
	private LinkedList<float[]> _laBuffer = new LinkedList<float[]>();
	/**
	 * gravity
	 */
	private LinkedList<float[]> _gravBuffer = new LinkedList<float[]>();
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
	
	/**
	 * linear acc in world frame, get from la&&rot
	 */
	private LinkedList<float[]> _laWfBuffer=new LinkedList<float[]>();
	
	/**
	 * 2015-4-8 09:44:49�� ����ź�ǿ���@������
	 * Ŀǰֻ�õ� _signalStrength[0]==cellID, [1]==strength;
	 * ��ʱ�������� _aTsBuffer;
	 */
	private LinkedList<float[]> _signalStrengthBuffer=new LinkedList<float[]>();
	
	
	private LinkedList<Double> _aTsBuffer=new LinkedList<Double>();
	private LinkedList<Double> _gTsBuffer=new LinkedList<Double>();
	private LinkedList<Double> _mTsBuffer=new LinkedList<Double>();
	private LinkedList<Double> _rTsBuffer=new LinkedList<Double>();
	private LinkedList<Double> _laTsBuffer=new LinkedList<Double>();
	
	//��������
	private LinkedList<Double> _laWfTsBuffer=_laTsBuffer;
	
//	private LinkedList<Long> _tsBuffer=new LinkedList<Long>();
	
	//epoch time in UTC, in millis, �ǵ�Ҫת����
	private double _baseTimestamp=0;
	private long _beginTimeInNano=0;

	public class MySensorData {
		
		public MySensorData() {
		}

		public LinkedList<float[]> getAbuf() {
//			return _abuf;
			return _aBuffer;
		}

		public LinkedList<float[]> getGbuf() {
//			return _gbuf;
			return _gyroBuffer;
		}

		public LinkedList<float[]> getMbuf() {
//			return _mbuf;
			return _mBuffer;
		}

		public LinkedList<float[]> getRbuf() {
//			return _rbuf;
			return _rotBuffer;
		}
		
//		2015-4-8 09:49:47
		public LinkedList<float[]> getBSSSbuf() {
			return _signalStrengthBuffer;
		}
		
		public LinkedList<Double> getATsBuf(){
			return _aTsBuffer;
		}

		public LinkedList<Double> getGTsBuf(){
			return _gTsBuffer;
		}

		public LinkedList<Double> getMTsBuf(){
			return _mTsBuffer;
		}
		
		public LinkedList<Double> getRTsBuf(){
			return _rTsBuffer;
		}
		
//		public void clearAllBuf() {
//			MySensorListener.this.clearAllBuf();
//		}

	}//MySensorData

	private MySensorData _sensorData = new MySensorData();

	public MySensorListener() {
//		//2015-4-8 10:31:49�� ���� gsm �źŷ���ȫ�֣� ���� un/register ��
//		PhoneStateListener psListener = new PhoneStateListener(){
//			@Override
//			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
//				// TODO Auto-generated method stub
//				super.onSignalStrengthsChanged(signalStrength);
//				if(signalStrength.isGsm()){
//					_signalStrength = signalStrength.getGsmSignalStrength();
//				}
//			}
//		};
		
	}//default ctor
	
	//2015-4-8 10:49:47
	public void setTelephonyManager(TelephonyManager tManager){
		_tManager = tManager;
		
		//���� gsm �źŷ���ȫ�֣� ���� un/register ��
		PhoneStateListener psListener = new PhoneStateListener(){
			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				// TODO Auto-generated method stub
				super.onSignalStrengthsChanged(signalStrength);
				System.out.println("onSignalStrengthsChanged, signalStrength: "+signalStrength);
				
				if(signalStrength.isGsm()){
					_signalStrength = signalStrength.getGsmSignalStrength();
				}
				
				//���� onSensorChanged ��Ῠ UI�� ���Է������ 
				//һ�� onSignalStrengthsChanged �ܻ��Ƚ������Σ� ���ص�����ȫ��δ changed
				GsmCellLocation location = (GsmCellLocation) _tManager.getCellLocation();
				_cellId = location.getCid();
				
			}
		};
		
		_tManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}//setTelephonyManager
	
	boolean someBufFull(){
		int curMax=Math.max(_aBuffer.size(), _gyroBuffer.size());
		curMax=Math.max(curMax, _mBuffer.size());
		curMax=Math.max(curMax, _rotBuffer.size());
		if(curMax>_maxBufSize)
			return true;
		return false;
	}//someBufFull
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		//OpenCameraZC.Preview.startCaptureSensor����ô�ã� _mainActivity._listener._allowStoreData = true;
		if(!_allowStoreData)
			return;
		
		 System.out.println("onSensorChanged");
		
		if(someBufFull())
			clearAllBuf();
		
		int eType = event.sensor.getType();
		float[] values = event.values.clone();
		
		//α������ֻ���Ƭ
		long ts=event.timestamp;
		System.out.println("System.currentTimeMillis(), e.ts: "+_allowStoreData+System.currentTimeMillis()+", "+event.timestamp+", "+eType+", "+_timeStamp);
		
		if(_isFirstFrame){
			_isFirstFrame=false;
			
			_beginTimeInNano=ts;
		}
		double epochTime=_baseTimestamp*Consts.MS2S+(ts-_beginTimeInNano)*Consts.NS2S;
		System.out.println("epochTime: "+epochTime);
		
		
		
//		if(_timeStamp==INVALID){
//			System.out.println("_timeStamp==INVALID");
//			_timeStamp=ts;
//		}
//		
//		if(ts-_timeStamp<0)
//			System.out.println("=======================");
//		
//		if(_timeStamp!=ts){
//			System.out.println("_timeStamp!=ts, "+_timeStamp+", "+ts+", "+eType);
//			if(_sensorCnt>=_sensorNum){
//				System.out.println("_sensorCnt>=_sensorNum");
//				offerBuffers();
//			}
//			else
//				System.out.println("_timeStamp!=ts && _sensorCnt<_sensorNum");
//			_timeStamp=ts;
//			_sensorCnt=0;
//		}
//		else
//			System.out.println("_timeStamp==ts, "+ts);
//			
//		addValidValues(eType, values);
		

		if (eType == Sensor.TYPE_ACCELEROMETER) {
			if(_aIsFirstFrame){
				_aIsFirstFrame=false;
				return;
			}
			_aBuffer.offer(values);
			_aTsBuffer.offer(epochTime);
			
			//2015-4-8 10:01:02
			//_tManager.getCellLocation() ��������ῨUI�� ��֪��Ϊʲô��
//			GsmCellLocation location = (GsmCellLocation) _tManager.getCellLocation();
//			_cellId = location.getCid();
			if (_cellId != -1) {
				float[] ssbuf = { _cellId, _signalStrength, 0 };
				_signalStrengthBuffer.offer(ssbuf);
			}
			System.out.println("onSensorChanged TYPE_ACCELEROMETER: "+values[0]+","+values[1]+","+values[2]+"; cid & BSSS: "+_cellId+","+_signalStrength);
		} else if (eType == Sensor.TYPE_LINEAR_ACCELERATION) {
			if(_laIsFirstFrame){
				_laIsFirstFrame=false;
				return;
			}
			_laBuffer.offer(values);
			_laTsBuffer.offer(epochTime);
			
			//ȫ������� linear acc Ҳ����������
			float[] rotMat=new float[9];
			SensorManager.getRotationMatrixFromVector(rotMat, _lastRotVec);

			float[] res = new float[3];
			for (int i = 0; i < 3; i++) {
				int idx = 3 * i;
				res[i] = rotMat[idx] * values[0] + rotMat[idx + 1] * values[1]
						+ rotMat[idx + 2] * values[2];
			}
			
			
//		} else if (eType == Sensor.TYPE_GRAVITY) {
//			_gBuffer.offer(values);
////			_gTsBuffer.offer(epochTime);
		} else if (eType == Sensor.TYPE_MAGNETIC_FIELD) {
			if(_mIsFirstFrame){
				_mIsFirstFrame=false;
				return;
			}
			_mBuffer.offer(values);
			_mTsBuffer.offer(epochTime);
		} else if (eType == Sensor.TYPE_ORIENTATION) {
			// do nothing
		} else if (eType == Sensor.TYPE_GYROSCOPE) {
			if(_gIsFirstFrame){
				_gIsFirstFrame=false;
				return;
			}
			_gyroBuffer.offer(values);
			_gTsBuffer.offer(epochTime);
		} else if (eType == Sensor.TYPE_ROTATION_VECTOR) {
			if(_rIsFirstFrame){
				_rIsFirstFrame=false;
				return;
			}
			_rotBuffer.offer(values);
			_rTsBuffer.offer(epochTime);
			System.out.println("values.length:= "+values.length+", "+values[0]+", "+values[01]+", "+values[02]);	//==3
			
			_lastRotVec=values;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	public LinkedList<float[]> getAccDataBuf() {
		return _aBuffer;
	}

	public LinkedList<float[]> getGravityDataBuf() {
		return _gravBuffer;
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

	public boolean registerWithSensorManager(SensorManager sm, int rate) {
		_baseTimestamp=System.currentTimeMillis();
		
//		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GRAVITY),
//				rate);
//		sm.registerListener(this,
//		sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), rate);

		if (!(
				sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate) && 
				sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate) && 
				sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), rate) && 
				sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), rate)))
			return false;
		return true;
	}

	public double get_baseTimestamp() {
		return _baseTimestamp;
	}

	public void set_baseTimestamp(double baseTimestamp) {
		this._baseTimestamp = baseTimestamp;
		this._isFirstFrame=true;
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
//		_isFirstFrame=true;
//		
//		_aIsFirstFrame=true;
//		_gIsFirstFrame=true;
//		_mIsFirstFrame=true;
//		_rIsFirstFrame=true;

		_aBuffer.clear();
		_laBuffer.clear();
		_gravBuffer.clear();
		_mBuffer.clear();
		_gyroBuffer.clear();
		_rotBuffer.clear();
		
		//2015-4-8 10:30:15
		_signalStrengthBuffer.clear();

//		_tsBuffer.clear();
		
		_aTsBuffer.clear();
		_gTsBuffer.clear();
		_mTsBuffer.clear();
		_rTsBuffer.clear();
		
	}
	
	public void reset(){
		_timeStamp=INVALID;
		_sensorCnt=0;
		
		_baseTimestamp=0;
		_beginTimeInNano=0;
		
		_isFirstFrame=true;
		_allowStoreData=false;
		
		_aIsFirstFrame=true;
		_gIsFirstFrame=true;
		_mIsFirstFrame=true;
		_rIsFirstFrame=true;
		
		//2015-4-8 10:29:44
		//IMU���������� onSignalStrengthsChanged �޷������������������ﲻ������ _cellId & _signalStrength
//		_cellId = -1;
//		_signalStrength = 0;
		
		clearAllBuf();
	}

}// MySensorListener

