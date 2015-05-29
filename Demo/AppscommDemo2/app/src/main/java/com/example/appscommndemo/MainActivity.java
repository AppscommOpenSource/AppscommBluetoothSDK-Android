package com.example.appscommndemo;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.appscomm.bleutils.BluetoothLeService;
import com.appscomm.bleutils.Pedometer_TypeInfo;
import com.appscomm.bleutils.ProtocolParser;
import com.appscomm.bleutils.SportsData;
import com.example.appscommdemo.R;

import java.util.Arrays;




/**
 * @author glin2
 *
 */
public class MainActivity extends Activity  {



	private TextView tv_addr,tv_log,tv_devName;
	private Button btn_scan,btn_test;
	private Handler mHandler;
	private static final int SCAN_MAXTIME = 10000; // 广播扫描的时间
	private static final int SCAN_TIMEOUT = 8001; // 超时消息
	private BluetoothAdapter mBluetoothAdapter;
	private static final String TAG = "MainActivity";
	private String mDeviceAddress = "";
	private boolean needScan = true;
	private boolean isScanning = false; //是否在扫描状态
    private Spinner spList;
	private BluetoothLeService mBluetoothLeService;   //蓝牙服务类

    private ArrayAdapter<String> devListAdapter;

    private LocalBroadcastManager localBroadcastManager;
	private int sendOrderIndex =0 ; //发送命令的序号
	
	
	
	
	
	
	
	/**
	 * 绑定蓝牙服务和广播，Activity初始化需绑定蓝牙服务。
	 */
	public void bindLeService() {
		
		Intent bleService = new Intent(this, BluetoothLeService.class);
		bindService(bleService, mServiceConnection, BIND_AUTO_CREATE);
        localBroadcastManager.registerReceiver(mGattUpdateReceiver,
                BluetoothLeService.makeGattUpdateIntentFilter());

	}

	
	
	
   /**
 *   取消绑定蓝牙服务和广播
 */
public void unbindService()
   {
	
	 
	   
	   	unbindService(mServiceConnection);
       localBroadcastManager.unregisterReceiver(mGattUpdateReceiver);
		
		mBluetoothLeService = null;
   }
	

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
            .getService();


            mBluetoothLeService.setPedometerType(Pedometer_TypeInfo.Pedometer_Type.L11);
			Log.i(TAG, "BLEService onServiceConnected()");

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			 mBluetoothLeService = null;
		}
	};
	
	
	
	
	
	/**
	 * 接收蓝牙服务传入的事件消息，或者是接收到的蓝牙数据
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
		
			Log.d(TAG, "receive BLE Action :" + action);
			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
		     //发现蓝牙服务，这时可以发送数据
			//	tv_log.append("发现蓝牙服务完成，发送同步时间命令\n");
			//	mBluetoothLeService.setSynTime();

                	tv_log.append("发现蓝牙服务完成，发送获取设备WatchID命令\n");
                	mBluetoothLeService.getWatchId();

			} 
			else if (BluetoothLeService.ACTION_GATT_SERVICES_TIMEOUT
					.equals(action)) {
			 //蓝牙通讯超时，这里可以重发未响应的命令	
				tv_log.append("蓝牙命令超时.......\n");	
				
			}
			
			else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
			 //蓝牙断开	
				tv_log.append("蓝牙连接断开.......\n");		
				
			}
			else if (BluetoothLeService.ACTION_DATA_AVAILABLE
					.equals(action) )
					{
			  //接收到蓝牙数据
		
				byte[] bytes = intent
						.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				 tv_log.append( "接收到数据:" + ProtocolParser.bytes2HexString(bytes)+"\n");
				 
				 pasrseData(bytes);
			
				
			}
		
		}		
	};

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_addr = (TextView) findViewById(R.id.tv_addr);
        tv_devName = (TextView) findViewById(R.id.tvDevName);
		tv_log = (TextView) findViewById(R.id.tv_log);
		
		btn_scan = (Button) findViewById(R.id.btn_scan);

		btn_test = (Button) findViewById(R.id.btn_test);
        spList  = (Spinner)findViewById (R.id.spList);
		
		btn_test.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				test_proc();
				
			}
		});



        devListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,  Arrays.asList(new String[]{Pedometer_TypeInfo.Pedometer_Type.values()[0].toString()
                , Pedometer_TypeInfo.Pedometer_Type.values()[1].toString(), Pedometer_TypeInfo.Pedometer_Type.values()[2].toString(),Pedometer_TypeInfo.Pedometer_Type.values()[3].toString()
                , Pedometer_TypeInfo.Pedometer_Type.values()[4].toString(), Pedometer_TypeInfo.Pedometer_Type.values()[5].toString(),Pedometer_TypeInfo.Pedometer_Type.values()[6].toString()}));

        spList.setAdapter(devListAdapter);

        spList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothLeService.setPedometerType(Pedometer_TypeInfo.Pedometer_Type.values()[i]);
                Toast.makeText(getApplicationContext(),"当前蓝牙通讯协议切换为:"+Pedometer_TypeInfo.Pedometer_Type.values()[i].toString(),Toast.LENGTH_LONG).show();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });





        localBroadcastManager = LocalBroadcastManager.getInstance(this);

		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter(); // 获取蓝牙适配器

		if (mBluetoothAdapter == null) {
			Toast.makeText(MainActivity.this, "无蓝牙适配器!", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		
		bindLeService();
		//启动蓝牙通讯service
		

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
		}

		btn_scan.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
             //    
				if (isScanning) return;
			//	Toast.makeText(MainActivity.this, "请点亮设备", Toast.LENGTH_SHORT).show();
                scanBLE(false);
				mDeviceAddress = "";
				tv_addr.setText("");
                tv_devName.setText("");
				

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanBLE(true);
                    }
                },2000);

				
				// TODO Auto-generated method stub

			}
		});

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {

				case SCAN_TIMEOUT:
					scanBLE(false); // 停止扫描
					if ("".equals(mDeviceAddress))
					{
					//	Toast.makeText(MainActivity.this, "没有扫描到蓝牙设备", Toast.LENGTH_SHORT).show();
					}
					break;

				default:
					break;

				}
			}

		};

	}

	
	
	protected void pasrseData(byte[] bytes) {
		
		if (null == bytes) return;
		
		
		if (bytes.length == 5 && bytes[0] == 0x6e && bytes[1] == 0x01 && bytes[2] == 0x00
				&& bytes[4] == (byte) 0x8f) {
		//电量返回的结果
			
			
			
		}
		
		else  if ( (bytes.length == 6) && bytes[0] == 0x6e && bytes[1] == 0x01    && bytes[2] == 0x01 
				&& bytes[5] == (byte) 0x8f )
		{
			//响应消息应答,具体看通讯协议
			
			
			
			tv_log.append("解析数据 收到应答 ,命令码:  0x" + String.format("%02x", bytes[3])  + "     结果： " +showResult(bytes[4]) +"\n" ) ;
			
			switch (bytes[3]) {
			case 0x15:  //时间时间返回




			 	tv_log.append("设置12小时制，Km显示\n");
				mBluetoothLeService.setTimeType(false,true,false,false,0); //设置12小时制，Km显示

				break;
				
			
				
			case 0x34:  //时间格式和距离单位设置返回
				tv_log.append("删除7:10分的一个提醒\n");  
				 mBluetoothLeService.deleteAReminder(7, 10); //删除7:10分的一个提醒
				  
				break;	

			case 0x09:  //删除提醒返回
				tv_log.append("添加一个 7:10清醒的提醒（星期一和星期三）\n");  
				mBluetoothLeService.addAReminder(4, 7, 10, "00000011"); //添加一个 7:10清醒的提醒（星期一和星期三）
				
				break;
			
			case 0x40 :   //添加提醒返回
				tv_log.append("设置 个人信息   男  19880214， 身高170  体重60\n");  
				mBluetoothLeService.SynPersonData(0, 1988, 2, 14, 170, 60);
				  //设置 个人信息   男  19880214， 身高170  体重60
				
			
			 break;
			 
			case 0x0c : //设置个人信息返回
				tv_log.append("设置目标10000步\n");
				mBluetoothLeService.setGoalSteps(10000); 
				   //设置目标10000步
				break;
			
				
			case 0x32 :
				tv_log.append("获取当日运动汇总\n");
				mBluetoothLeService.getSportDataTotal(); 
				break;
				
			case 0x0d :  //目标设置完成
				tv_log.append("设置自动删除数据模式\n");
				mBluetoothLeService.setManualMode(3);
				break;
				
			case 0x06 :  //运动详细数据传输完成返回
				tv_log.append("获取运动详细数据完成\n");
				
				break;
			default:
				break;
			}
			
		}

        else  if (bytes.length == 24 && bytes[0] == 0x6e && bytes[2] == 0x04
                && bytes[23] == (byte) 0x8f)
        {


            tv_log.append("获取的DN号为：" + ProtocolParser.parseDeviceDN(bytes)+"\n");
            tv_log.append("发送同步时间命令\n");
            	mBluetoothLeService.setSynTime();

        }


		else if (bytes.length == 20 && bytes[0] == 0x6e   && bytes[1] == 0x01 && bytes[2] == 0x0F
				&& bytes[19] == (byte) 0x8f) 
		{
			//运动汇总数据返回
			SportsData sData = ProtocolParser.parseSportTotalData(bytes);
			
			if (null==sData)
			{
				tv_log.append("获取运动汇总数据为空\n");
				
			}
			else {
				
				tv_log.append("获取运动汇总数据:  步数:"+ sData.steps + " ,卡路里: " + sData.cal  +   " \n");	
			}
			
			
			tv_log.append("获取运动明细数据..\n"); 
			mBluetoothLeService.getSportDataDetail(); //获取运动明细数据
		}
		
		else if

                ( (bytes.length == 21 && bytes[0] == 0x6e && bytes[2] == 0x05 && bytes[20] == (byte)0x8f ) //L28S/C返回
		          || (bytes.length == 19 && bytes[0] == 0x6e && bytes[2] == 0x05                           //L11/L28T/W/H 返回
                        && bytes[18] == (byte) 0x8f)

                )
        {
			//运动详细数据返回
			
			SportsData sData = ProtocolParser.parseSportDetailData(bytes);
			
			if (null==sData)
			{
				tv_log.append("获取运动详细数据为空\n");
				
			}
			else {
				
				tv_log.append("获取运动详细数据:  步数:"+ sData.steps + " ,卡路里: " + sData.cal  +   " \n");	
			}
				
			
			
			
		}

		
	}


    
	private String showResult(byte res)
	{
		String s ="";
		switch (res) {
		case 0:
			   s ="成功";
			break;

		case 1:
			   s ="失败";
			break;
		
		case 2:
			   s ="非法命令";
			break;

			


		default:
			s = String.format("%x", res);
			break;
		}
		
		return s;
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unbindService();
	}

	
	protected void test_proc() {
		// TODO Auto-generated method stub
		tv_log.setText("");
		
		if (mBluetoothLeService ==null)
		{
			Toast.makeText(this, "蓝牙服务未绑定!",Toast.LENGTH_SHORT ).show();
			return;
			
		}
		
		
		if (mDeviceAddress.length()<12)
		{
			Toast.makeText(this, "没有指定设备，请先扫描!",Toast.LENGTH_SHORT ).show();
			return;
			
		}
		
		
		
			
			//指定MAC地址连接
			tv_log.append("重新连接设备：" + mDeviceAddress +"\n");
			mBluetoothLeService.connect(mDeviceAddress,"");
		
		
	}


	/**
	 * BLE扫描回调结果
	 * 蓝牙扫描过程不是每次通讯前的必须操作步骤，如果已有蓝牙设备的MAC地址，无需再进行扫描步骤
	 */
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@SuppressLint("NewApi")
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

                    Log.d(TAG,"find dev:"+ device.getName() + "rssi:"+ rssi);

					if (null != device.getName() && device.getName().length() > 2 && BluetoothLeService.devNamePrefix.toUpperCase().equals(device.getName().toUpperCase().subSequence(0, BluetoothLeService.devNamePrefix.length()))) {

						if (!needScan)
							return;
						mDeviceAddress = device.getAddress();
						tv_addr.setText(mDeviceAddress);
                        tv_devName.setText(device.getName()+ "       ("+rssi+"dB)");
						Log.d(TAG, "find a  device:" + mDeviceAddress);
				//		Toast.makeText(MainActivity.this, "发现设备："+device.getName() + " MAC:"+ mDeviceAddress, Toast.LENGTH_SHORT).show();
						scanBLE(false);

					}

				}
			});
		}
	};

	
	/**
	 * @param enable  true:开启扫描  
	 * 				 false: 停止扫描
	 */
	private void scanBLE(boolean enable)
	{
		if (enable)
		{
			if (isScanning) return;  //正在扫描状态退出
			
		
			needScan = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			isScanning = true;
			mHandler.sendEmptyMessageDelayed(SCAN_TIMEOUT, SCAN_MAXTIME);
		}
		else {
			needScan = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback); // 停止扫描
			isScanning = false;
		}
		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



}
