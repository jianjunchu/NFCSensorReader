package cn.nfcsolution.resistance;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.aofei.nfc.TagUtil;

public final class Tools {
	// 1.写32hex块，数据为0xAAAAAAAA
	public static boolean write32H(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		boolean writeSuccess = false;
		try {
			if (intent != null && mTagUtil != null) {
				// 写标签：中间2个参数是页面地址address, content(byte[])
				byte addr = 0x32;
				byte[] content = {(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
				writeSuccess = mTagUtil.writeTag(intent, addr, content, isCheckSUM);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return writeSuccess;
	}
	
	// 2.读24hex块，返回数据data_read，4个字节
	public static byte[] read24H(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		try {
			if (intent != null && mTagUtil != null) {
				// 写标签：中间2个参数是address,页面地址
				byte addr = 0x24;
				return mTagUtil.readOnePage(intent, addr, isCheckSUM);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	public static final List<String> hexCache = new ArrayList<String>();
//	public static final List<Integer> bitsCache = new ArrayList<Integer>();
	
	// 保留低15位
	public static Integer trim15bits(byte[] data_read) {
		if (data_read != null && data_read.length >= 2) {
			byte high = data_read[1];
			byte low = data_read[0];
			int data = (high & 0x7f);
			int data2 = data << 8;
			int data3 = (data2) | (low & 0xff);
			// Print for testing
//			bitsCache.add(data3);
//			hexCache.add("0x" + BytesHexStrTranslate.bytesToHex(data_read));
			
			return data3;
		}
//		bitsCache.add(0);
//		hexCache.add("0x00");
		return 0;
	}
	
	public static float readRValue(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		byte[] dataRead = read24H(mTagUtil, intent, isCheckSUM);
		int data = Tools.trim15bits(dataRead);
		float f = 0.0f;
		if (data != 0) {
			f = (((32767.0f / data) - 1) * MainActivity.Rm);
		}
		return f;
	}
	
	public static void readNoAuthPages(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		if (mTagUtil.lockPageAll(intent, isCheckSUM)) {
			try {
				byte[] bytes = mTagUtil.readAllPages(intent, isCheckSUM);
				for (byte b : bytes) {
					Log.i("Main", "read byte: " + b);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i("Main", "lock page failed!");
		}
	}
	
	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public static void shortToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
