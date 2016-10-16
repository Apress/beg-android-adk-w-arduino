package project.twelve.adk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class ProjectTwelveActivity extends Activity {

	private static final String TAG = ProjectTwelveActivity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private PendingIntent smsSentIntent;
	private PendingIntent logFileWrittenIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private static final byte COMMAND_ALARM = 0x9;
	private static final byte ALARM_TYPE_TILT_SWITCH = 0x1;
	private static final byte ALARM_OFF = 0x0;
	private static final byte ALARM_ON = 0x1;
	
	private static final String SMS_DESTINATION = "015785741105";
	private static final String SMS_SENT_ACTION = "SMS_SENT";
	private static final String LOG_FILE_WRITTEN_ACTION = "LOG_FILE_WRITTEN";
	
	private PackageManager packageManager;
	private boolean hasTelephony;

	private TextView alarmTextView;
	private TextView smsTextView;
	private TextView logTextView;
	private LinearLayout linearLayout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		smsSentIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				SMS_SENT_ACTION), 0);
		logFileWrittenIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				LOG_FILE_WRITTEN_ACTION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(SMS_SENT_ACTION);
		filter.addAction(LOG_FILE_WRITTEN_ACTION);
		registerReceiver(broadcastReceiver, filter);
		
		packageManager = getPackageManager();
		hasTelephony = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

		setContentView(R.layout.main);
		linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
		alarmTextView = (TextView) findViewById(R.id.alarm_text);
		smsTextView = (TextView) findViewById(R.id.sms_text);
		logTextView = (TextView) findViewById(R.id.log_text);
	}

	/**
	 * Called when the activity is resumed from its paused state and immediately
	 * after onCreate().
	 */
	@Override
	public void onResume() {
		super.onResume();

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
			} else if (SMS_SENT_ACTION.equals(action)) {
				smsTextView.setText(R.string.sms_sent_message);
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

					if (buffer[1] == ALARM_TYPE_TILT_SWITCH) {
						final byte alarmState = buffer[2];
						final String alarmMessage = getString(R.string.alarm_message, getString(R.string.alarm_type_tilt_switch));
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if(alarmState == ALARM_ON) {
									linearLayout.setBackgroundColor(Color.RED);
									alarmTextView.setText(alarmMessage);
								} else if(alarmState == ALARM_OFF) {
									linearLayout.setBackgroundColor(Color.WHITE);
									alarmTextView.setText(R.string.alarm_reset_message);
									smsTextView.setText("");
									logTextView.setText("");
								}
							}
						});
						if(alarmState == ALARM_ON) {
							sendSMS(alarmMessage);
							writeToLogFile(new StringBuilder(alarmMessage).append(" - ").append(new Date()).toString());
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
	
	private void sendSMS(String smsText) {
		if(hasTelephony) {
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage(SMS_DESTINATION, null, smsText, smsSentIntent, null);
			Log.i(TAG, "SMS sent");
		}
	}
	
	private void writeToLogFile(String logMessage) {
		File logDirectory = getExternalLogFilesDir();
		if(logDirectory != null) {
			File logFile = new File(logDirectory, "ProjectTwelveLog.txt");
			if(!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					Log.d(TAG, "Log File could not be created.", e);
				}
			}
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
	
	private File getExternalLogFilesDir() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return getExternalFilesDir(null);
		} else  {
			return null;
		}
	}
}