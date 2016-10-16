package project.nine.adk;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class ProjectNineActivity extends Activity {

	private static final String TAG = ProjectNineActivity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private static final byte COMMAND_TOUCH_SENSOR = 0x6;
	private static final byte SENSOR_ID = 0x0;

	private LinearLayout linearLayout;
	private TextView buzzerIdentifierTextView;
	
	private Vibrator vibrator;
	private boolean isVibrating;
	
	private SoundPool soundPool;
	private boolean isSoundPlaying;
	private int soundId;
	
	private float streamVolumeMax;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		setContentView(R.layout.main);
		linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
		buzzerIdentifierTextView = (TextView) findViewById(R.id.buzzer_identifier);
		
		vibrator = ((Vibrator) getSystemService(VIBRATOR_SERVICE));
		
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		soundId = soundPool.load(this, R.raw.buzzer, 1);
		
		AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
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
				synchronized (mUsbReceiver) {
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
		stopVibrate();
		stopSound();
	}

	/**
	 * Called when the activity is no longer needed prior to being removed from
	 * the activity stack.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		releaseSoundPool();
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
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
				case COMMAND_TOUCH_SENSOR:

					if (buffer[1] == SENSOR_ID) {
						final byte buzzerId = buffer[1];
						final boolean buzzerIsPressed = buffer[2] == 0x1;
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if(buzzerIsPressed) {
									linearLayout.setBackgroundColor(Color.RED);
									buzzerIdentifierTextView.setText(getString(R.string.touch_button_identifier, buzzerId));
									startVibrate();
									playSound();
								} else {
									linearLayout.setBackgroundColor(Color.WHITE);
									buzzerIdentifierTextView.setText("");
									stopVibrate();
									stopSound();
								}
							}
						});
					}
					break;

				default:
					Log.d(TAG, "unknown msg: " + buffer[0]);
					break;
				}
			}
		}
	};
	
	private void startVibrate() {
		if(vibrator != null && !isVibrating) {
			isVibrating = true;
			vibrator.vibrate(new long[]{0, 1000, 250}, 0);
		}
	}
	
	private void stopVibrate() {
		if(vibrator != null && isVibrating) {
			isVibrating = false;
			vibrator.cancel();
		}
	}
	
	private void playSound() {
		if(!isSoundPlaying) {
			soundPool.play(soundId, streamVolumeMax, streamVolumeMax, 1, 0, 1.0F);
			isSoundPlaying = true;
		}
	}
	
	private void stopSound() {
		if(isSoundPlaying) {
			soundPool.stop(soundId);
			isSoundPlaying = false;
		}
	}
	
	private void releaseSoundPool() {
		if(soundPool != null) {
			stopSound();
			soundPool.release();
			soundPool = null;
		}
	}
}