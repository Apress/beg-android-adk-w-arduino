package project.thirteen.adk;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class ProjectThirteenActivity extends Activity {

	private static final String TAG = ProjectThirteenActivity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private PendingIntent photoTakenIntent;
	private PendingIntent logFileWrittenIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private static final byte COMMAND_ALARM = 0x9;
	private static final byte ALARM_TYPE_IR_LIGHT_BARRIER = 0x2;
	private static final byte ALARM_OFF = 0x0;
	private static final byte ALARM_ON = 0x1;
	
	private static final String PHOTO_TAKEN_ACTION = "PHOTO_TAKEN";
	private static final String LOG_FILE_WRITTEN_ACTION = "LOG_FILE_WRITTEN";
	
	private PackageManager packageManager;
	private boolean hasFrontCamera;
	private boolean hasBackCamera;
	
	private Camera camera;
	private SurfaceView surfaceView;

	private TextView alarmTextView;
	private TextView photoTakenTextView;
	private TextView logTextView;
	private LinearLayout linearLayout;
	private FrameLayout frameLayout; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		photoTakenIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				PHOTO_TAKEN_ACTION), 0);
		logFileWrittenIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				LOG_FILE_WRITTEN_ACTION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(PHOTO_TAKEN_ACTION);
		filter.addAction(LOG_FILE_WRITTEN_ACTION);
		registerReceiver(broadcastReceiver, filter);
		
		packageManager = getPackageManager();
		hasFrontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		hasBackCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);

		setContentView(R.layout.main);
		linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
		frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
		alarmTextView = (TextView) findViewById(R.id.alarm_text);
		photoTakenTextView = (TextView) findViewById(R.id.photo_taken_text);
		logTextView = (TextView) findViewById(R.id.log_text);
	}

	/**
	 * Called when the activity is resumed from its paused state and immediately
	 * after onCreate().
	 */
	@Override
	public void onResume() {
		super.onResume();

		camera = getCamera();
		surfaceView = new CameraPreview(this, camera);
		frameLayout.addView(surfaceView);
		
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (broadcastReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	/** Called when the activity is paused by the system. */
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
		if(camera != null) {
			camera.release();
			camera = null;
			frameLayout.removeAllViews();
		}
	}

	/**
	 * Called when the activity is no longer needed prior to being removed from
	 * the activity stack.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			} else if (PHOTO_TAKEN_ACTION.equals(action)) {
				photoTakenTextView.setText(R.string.photo_taken_message);
			} else if (LOG_FILE_WRITTEN_ACTION.equals(action)) {
				logTextView.setText(R.string.log_written_message);
			}
		}
	};

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, commRunnable, TAG);
			thread.start();
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	Runnable commRunnable = new Runnable() {

		@Override
		public void run() {
			int ret = 0;
			byte[] buffer = new byte[3];

			while (ret >= 0) {
				try {
					ret = mInputStream.read(buffer);
				} catch (IOException e) {
					Log.e(TAG, "IOException", e);
					break;
				}

				switch (buffer[0]) {
				case COMMAND_ALARM:

					if (buffer[1] == ALARM_TYPE_IR_LIGHT_BARRIER) {
						final byte alarmState = buffer[2];
						final String alarmMessage = getString(R.string.alarm_message, getString(R.string.alarm_type_ir_light_barrier));
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if(alarmState == ALARM_ON) {
									linearLayout.setBackgroundColor(Color.RED);
									alarmTextView.setText(alarmMessage);
								} else if(alarmState == ALARM_OFF) {
									linearLayout.setBackgroundColor(Color.WHITE);
									alarmTextView.setText(R.string.alarm_reset_message);
									photoTakenTextView.setText("");
									logTextView.setText("");
								}
							}
						});
						if(alarmState == ALARM_ON) {
							takePhoto();
							writeToLogFile(new StringBuilder(alarmMessage).append(" - ").append(new Date()).toString());
						} else if(alarmState == ALARM_OFF){
							camera.startPreview();
						}
					}
					break;

				default:
					Log.d(TAG, "unknown msg: " + buffer[0]);
					break;
				}
			}
		}
	};
	
	private Camera getCamera(){
	    Camera camera = null;
	    try {
	    	if(hasFrontCamera) {
	    		int frontCameraId = getFrontCameraId();
	    		if(frontCameraId != -1) {
	    			camera = Camera.open(frontCameraId);
	    		}
	    	}
	    	if((camera == null) && hasBackCamera) {
	    		camera = Camera.open();
	    	}
	    }
	    catch (Exception e){
	        Log.d(TAG, "Camera could not be initialized.", e);
	    }
	    return camera;
	}
	
	private int getFrontCameraId() {
		int cameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(i, cameraInfo);
			if (CameraInfo.CAMERA_FACING_FRONT == cameraInfo.facing) {
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}
	
	private void takePhoto() {
		if(camera != null) {
			camera.takePicture(null, null, pictureTakenHandler);
		}
	}
	
	private PictureCallback pictureTakenHandler = new PictureCallback() {
		
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			writePictureDataToFile(data);
		}
	};
	
	private void writeToLogFile(String logMessage) {
		File logFile = getFile("ProjectThirteenLog.txt");
			if(logFile != null) {
			BufferedWriter bufferedWriter = null;
			try {
				bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
				bufferedWriter.write(logMessage);
				bufferedWriter.newLine();
				Log.d(TAG, "Written message to file: " + logFile.toURI());
				logFileWrittenIntent.send();
			} catch (IOException e) {
				Log.d(TAG, "Could not write to Log File.", e);
			} catch (CanceledException e) {
				Log.d(TAG, "LogFileWrittenIntent was cancelled.", e);
			} finally {
				if(bufferedWriter != null) {
					try {
						bufferedWriter.close();
					} catch (IOException e) {
						Log.d(TAG, "Could not close Log File.", e);
					}
				}
			}
		}
	}
	
	private void writePictureDataToFile(byte[] data) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String currentDateAndTime = dateFormat.format(new Date());
		File pictureFile = getFile(currentDateAndTime + ".jpg");
		if(pictureFile != null) {
			BufferedOutputStream bufferedOutputStream = null;
			try {
				bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(pictureFile));
				bufferedOutputStream.write(data);
				Log.d(TAG, "Written picture data to file: " + pictureFile.toURI());
				photoTakenIntent.send();
			} catch (IOException e) {
				Log.d(TAG, "Could not write to Picture File.", e);
			} catch (CanceledException e) {
				Log.d(TAG, "photoTakenIntent was cancelled.", e);
			} finally {
				if(bufferedOutputStream != null) {
					try {
						bufferedOutputStream.close();
					} catch (IOException e) {
						Log.d(TAG, "Could not close Picture File.", e);
					}
				}
			}
		}
	}
	
	private File getFile(String fileName) {
		File file = new File(getExternalDir(), fileName);
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Log.d(TAG, "File could not be created.", e);
			}
		}
		return file;
	}
	
	private File getExternalDir() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return getExternalFilesDir(null);
		} else  {
			return null;
		}
	}
}