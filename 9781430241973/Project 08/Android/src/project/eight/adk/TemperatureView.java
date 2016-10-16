package project.eight.adk;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TemperatureView extends View {
	private float currentTemperature;
	private Paint textPaint = new Paint();
	private Paint thermometerPaint = new Paint();
	private RectF thermometerOval = new RectF();
	private RectF thermometerRect = new RectF();
	
	private int availableWidth;
	private int availableHeight;
	
	private final float deviceDensity;
	
	private int ovalLeftBorder;
	private int ovalTopBorder;
	private int ovalRightBorder;
	private int ovalBottomBorder;
	
	private int rectLeftBorder;
	private int rectTopBorder;
	private int rectRightBorder;
	private int rectBottomBorder;

	public TemperatureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		textPaint.setColor(Color.BLACK);
		thermometerPaint.setColor(Color.RED);
		deviceDensity = getResources().getDisplayMetrics().density;
		TypedArray attributeArray = context.obtainStyledAttributes(attrs, R.styleable.temperature_view_attributes); 
		int textSize = attributeArray.getInt(R.styleable.temperature_view_attributes_textSize, 9);
		textSize = (int) (textSize * deviceDensity + 0.5f);
		textPaint.setTextSize(textSize);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		availableWidth = getMeasuredWidth();
		availableHeight = getMeasuredHeight();
		
		ovalLeftBorder = (availableWidth / 2) - (availableWidth / 10);
		ovalTopBorder = availableHeight - (availableHeight / 10) - (availableWidth / 5);
		ovalRightBorder = (availableWidth / 2) + (availableWidth / 10);
		ovalBottomBorder = availableHeight - (availableHeight / 10);
		//setup oval with its position centered horizontally and at the bottom of the screen
		thermometerOval.set(ovalLeftBorder, ovalTopBorder, ovalRightBorder, ovalBottomBorder);
		
		rectLeftBorder = (availableWidth / 2) - (availableWidth / 15);
		rectRightBorder = (availableWidth / 2) + (availableWidth / 15);
		rectBottomBorder = ovalBottomBorder - ((ovalBottomBorder - ovalTopBorder) / 2);
	}
	
	public void setCurrentTemperature(float currentTemperature) {
		this.currentTemperature = currentTemperature;
		//only draw a thermometer in the range of -50 to 50 degrees celsius 
		float thermometerRectTop = currentTemperature + 50;
		if(thermometerRectTop < 0) {
			thermometerRectTop = 0;
		} else if(thermometerRectTop > 100){
			thermometerRectTop = 100;
		}
		rectTopBorder = (int) (rectBottomBorder - (thermometerRectTop * (availableHeight / 140)));
		//update rect borders
		thermometerRect.set(rectLeftBorder, rectTopBorder, rectRightBorder, rectBottomBorder);
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//draw shapes
		canvas.drawOval(thermometerOval, thermometerPaint);
		canvas.drawRect(thermometerRect, thermometerPaint);
		//draw text in the upper left corner
		canvas.drawText(getContext().getString(R.string.temperature_value, currentTemperature), availableWidth / 10, availableHeight / 10, textPaint);
	}
	
}
