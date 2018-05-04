package cn.nfcsolution.resistance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aofei.nfc.TagUtil;

public class MainActivity extends Activity {
	private Button mBtnStart;
	private EditText mEdtTime;
	private TagUtil mTagUtil = null;
	private Intent mKeepIntent = null;
	private final boolean isCheckSUM = false;
	public static final float Rm = 47.7f;  // 47.7 k欧姆
	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;
	private LineChartView chart;
	private LineChartData data;
	private int numberOfPoints = 0;
	private float mMaxRm = Rm * 10;
	
	private final List<Float> randomNumbersTab = new ArrayList<Float>();
	
	private final boolean hasAxes = true;
	private final boolean hasAxesNames = true;
	private final boolean hasLines = true;
	private final boolean hasPoints = true;
	private final ValueShape shape = ValueShape.CIRCLE;
	private final boolean isFilled = false;
	private final boolean hasLabels = false;
	private final boolean isCubic = true;
	private final boolean hasLabelForSelected = false;
	private boolean pointsHaveDifferentColor;
	private Line mLine;
	private List<PointValue> mValues = new ArrayList<PointValue>();
	private List<Line> mLines;
	private int mLoopCounter = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initNfc();
		setupListener();
		initData();
		initChart();
		processIntent(getIntent());
	}

	private void initChart() {
		chart = (LineChartView) findViewById(R.id.chart);
		chart.setOnValueTouchListener(new ValueTouchListener(this));
		chart.setViewportCalculationEnabled(false);
		chart.setVisibility(8);
	}
	
	private void resetViewport(long maxmillseconds) {
		final Viewport v = new Viewport(chart.getMaximumViewport());
		v.bottom = 0;
		v.top = mMaxRm;
		v.left = 0;
		v.right = maxmillseconds + 1000;
		chart.setMaximumViewport(v);
		chart.setCurrentViewport(v);
	}
	
	private void generateValues(Float f) {
		randomNumbersTab.add(f);
		numberOfPoints = randomNumbersTab.size();
	}

	private void generateData(long timeSpanMill) {
		if (mLines == null) {
			mLines = new ArrayList<Line>();
		}
		
		if (mValues == null) {
			mValues = new ArrayList<PointValue>();
		}
		
		mValues.add(new PointValue(timeSpanMill, randomNumbersTab.get(numberOfPoints - 1)));
		
		if (mLine == null) {
			mLine = new Line(mValues);
			mLine.setColor(ChartUtils.COLORS[0]);
			mLine.setShape(shape);
			mLine.setCubic(isCubic);
			mLine.setFilled(isFilled);
			mLine.setHasLabels(hasLabels);
			mLine.setHasLabelsOnlyForSelected(hasLabelForSelected);
			mLine.setHasLines(hasLines);
			mLine.setHasPoints(hasPoints);
			// line.setHasGradientToTransparent(hasGradientToTransparent);
			if (pointsHaveDifferentColor) {
				mLine.setPointColor(ChartUtils.COLORS[(0 + 1)
						% ChartUtils.COLORS.length]);
			}
		}
		
		if (mLines.size() == 0) {
			mLines.add(mLine);
		}
		
		if (data == null) {
			data = new LineChartData(mLines);
			
			if (hasAxes) {
				Axis axisX = new Axis();
				Axis axisY = new Axis().setHasLines(true);
				if (hasAxesNames) {
					axisX.setName("时间（ms）");
					axisY.setName("电阻（kΩ）");
				}
				data.setAxisXBottom(axisX);
				data.setAxisYLeft(axisY);
			} else {
				data.setAxisXBottom(null);
				data.setAxisYLeft(null);
			}
			data.setBaseValue(Float.NEGATIVE_INFINITY);
		}
		
		chart.setLineChartData(data);
	}

	private void setupListener() {
		mBtnStart = (Button) findViewById(R.id.btnStart);
		mBtnStart.setOnClickListener(new JEscapeDoubleClickListener() {
			@Override
			public void onNoDoubleClick(View v) {
				doStart();
			}
		});

		mEdtTime = (EditText) findViewById(R.id.edtimer);
//		mTipView = (TextView) findViewById(R.id.runningtip1);
	}

	private void initData() {
		
	}

	protected void initNfc() {
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Tools.showToast(this, "你的设备不支持NFC!");
			finish();
			return;
		}
		try {
			if (!mNfcAdapter.isEnabled()) {
				Tools.showToast(this, "请进入系统设置开启NFC!");
				finish();
				return;
			}
		} catch (Exception e) {
			finish();
			return;
		}

		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()), 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mNfcAdapter != null)
			mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null,null);
		processIntent(getIntent());
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		mNfcAdapter.disableForegroundNdefPush(this);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		processIntent(intent);
	}
	
	private void processIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			boolean tagDiscovered = NfcAdapter.ACTION_TAG_DISCOVERED
					.equals(action);
			boolean techDiscovered = NfcAdapter.ACTION_TECH_DISCOVERED
					.equals(action);
			if (tagDiscovered || techDiscovered) {
				this.mKeepIntent = intent;
				if (mTagUtil == null) {
					try {
						mTagUtil = TagUtil.selectTag(intent, isCheckSUM);
						String uidString = TagUtil.getUid();
						Tools.showToast(MainActivity.this, "发现标签，ID=" + uidString);
//						int authAddress = mTagUtil.getAuthenticationAddr(intent, isCheckSUM);
//						if (authAddress > 48) {
//							Tools.showToast(MainActivity.this, uidString + "所有page不需要验证");
//						} else {
//							Tools.showToast(MainActivity.this, uidString + "需要验证page" + authAddress + "到48");
//						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void continuousDetectingADC(final int maxMillseconds) {
		new AsyncTask<Void, Float, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				long millStart = System.currentTimeMillis();
				while (System.currentTimeMillis() - millStart <= maxMillseconds) {
					boolean writeSuccess = Tools.write32H(mTagUtil, mKeepIntent, isCheckSUM);
					if (writeSuccess) {
						try {
							TimeUnit.MILLISECONDS.sleep(40);
							float f = Tools.readRValue(mTagUtil, mKeepIntent, isCheckSUM);
							Log.i("Main", "电阻值=" + f);
							generateValues(f);
							generateData(System.currentTimeMillis() - millStart);
							publishProgress(f);
						} catch (Exception e) {
							e.printStackTrace();
							generateValues(0.0f);
							generateData(System.currentTimeMillis() - millStart);
							publishProgress(0.0f);
							return null;
						}
					} else {
						generateValues(0.0f);
						generateData(System.currentTimeMillis() - millStart);
						publishProgress(0.0f);
						return null;
					}
				}
				return null;
			}
			
			@Override
			protected void onPreExecute() {
				chart.setVisibility(0);
				mLoopCounter = 0;
//				mTipView.setText("正在运行..");
				setTitle(R.string.app_name);
				resetViewport(maxMillseconds);
			}
			
			@Override
			protected void onProgressUpdate(Float... values) {
				mLoopCounter++;
				if (values != null && values.length > 0 && values[0] > mMaxRm) {
					mMaxRm = values[0] + Rm;
					resetViewport(maxMillseconds);
				}
			}
			
			@Override
			protected void onPostExecute(Void result) {
				setTitle(String.format("%s - 运行结束，共计算次数：%d次", getString(R.string.app_name), mLoopCounter));
				doStop();
			}
		}.execute();
	}
	
	private void doStart() {
//		hasStarted = true;
		if (mKeepIntent == null) {
			Tools.showToast(this, "未识别到标签靠近");
			return;
		}
		mBtnStart.setEnabled(false);
		mBtnStart.setText(R.string.stop);
		mBtnStart.setBackgroundResource(R.drawable.button_disabled_bg);
		mBtnStart.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_stop,
				0, 0, 0);
		mEdtTime.setEnabled(false);
		// Clear all data
		randomNumbersTab.clear();
		mValues.clear();
//		Tools.bitsCache.clear();
//		Tools.hexCache.clear();
		
		numberOfPoints = 0;
		// Add testing points one by one.
		String secStr = mEdtTime.getText().toString();
		int millseconds = Integer.parseInt(secStr) * 1000;
		continuousDetectingADC(millseconds);
	}
	
	private void doStop() {
		mBtnStart.setEnabled(true);
		mBtnStart.setText(R.string.start);
		mBtnStart.setBackgroundResource(R.drawable.button_enable_green_bg);
		mBtnStart.setCompoundDrawablesWithIntrinsicBounds(
				R.drawable.icon_start, 0, 0, 0);
		mEdtTime.setEnabled(true);
	}

	private class ValueTouchListener implements LineChartOnValueSelectListener {
		private final Context mContext;

		public ValueTouchListener(Context context) {
			this.mContext = context;
		}

		@Override
		public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
			Tools.showToast(mContext, String.format("第%d次电阻值: %.3f 千欧", (pointIndex+1), value.getY()));
			/*
			String addrContent = "";
			if (Tools.hexCache.size() > pointIndex) {
				addrContent = Tools.hexCache.get(pointIndex);
			}
			Tools.showToast(mContext, String.format("第%d次电阻值: %.3f 千欧\n24H地址内容：%s", 
					(pointIndex+1), value.getY(), addrContent));
			*/
		}

		@Override
		public void onValueDeselected() {

		}
	}
}
