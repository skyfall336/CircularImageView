package com.example.circularimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * 1.从xml中获取配置信息，尤其是半径和圆心 2.根据半径和圆心，在onMeasure中设置控件的大小为半径
 * 3.在onSizeChanged中根据得到的控件大小去裁剪图片，获得Bitmap 4.在onDraw中绘制bitmap
 * */

public class CircularImageView extends View {

	public final static String TAG = "CircularImageView";

	public Bitmap srcBitmap; // 未裁剪原图
	public Bitmap dstBitmap; // 裁剪后圆图
	public Bitmap scaleBitmap; // 最终显示的缩放图
	public Canvas myCanvas; // 用来裁剪scaleBitmap的画布
	public Paint mPaint; // 用来裁剪scaleBitmap的画笔
	public Paint borderPaint; // 用来裁剪边界的画笔
	public int width; // 控件宽度
	public int height; // 控件高度
	public int srcWidth; // 原始Bitmap宽度
	public int srcHeight; // 原始Bitmap高度
	public int radius; // 圆形头像半径,用来裁剪原始Bitmap，不是最终圆形Bitmap半径也不是用户指定的半径比例

	public int borderWidth; // 边框总宽度
	public int inBorderWidth = 0; // 内圆宽度，默认为0,不绘制
	public int betweenWidth; // 环瓤
	public int outBorderWidth = 0; // 外圆宽度，默认为0，不绘制

	public int borderColor; // 边框颜色
	public int inBorderColor = Color.BLACK; // 默认内圆为黑色
	public int betweenColor = Color.WHITE; // 默认环瓤为白色，如果未设置外圆和内圆，全为黑色
	public int outBorderColor = Color.BLACK; // 默认外圆为黑色

	public float centerScaleX = 0; // 头像中心点位置X轴比例
	public float centerScaleY = 0; // 头像中心点位置Y轴比例
	public float centerRadius; // 原始图片中指定的半径(比例)

	public final static PorterDuffXfermode modeInside; // 圆形头像中心保存下层
	public final static PorterDuffXfermode modeOutsize; // 环形边框保存上层

	static {
		modeInside = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
		modeOutsize = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
	}

	public CircularImageView(Context context) {
		super(context);
		init();
		initPaint();
	}

	public CircularImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// 获得配置信息
		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.CircularImage);
		int id = array.getResourceId(
				R.styleable.CircularImage_CircularImageSrc, 0);
		if (id != 0) {
			srcBitmap = BitmapFactory.decodeResource(getResources(), id);
			srcWidth = srcBitmap.getWidth();
			srcHeight = srcBitmap.getHeight();
		}
		// 圆环颜色分布
		borderColor = array.getColor(R.styleable.CircularImage_BorderColor,
				Color.BLACK);
		inBorderColor = array.getColor(R.styleable.CircularImage_InBorderColor,
				Color.BLACK);
		betweenColor = array.getColor(R.styleable.CircularImage_BetweenColor,
				Color.WHITE);
		outBorderColor = array.getColor(
				R.styleable.CircularImage_OutBorderColor, Color.BLACK);
		// 圆环宽度分布
		borderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_BorderWidth, 0);
		inBorderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_InBorderWidth, 0);
		betweenWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_BetweenWidth, 0);
		outBorderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_OutBorderWidth, 0);
		// 圆形中心点位置
		centerScaleX = array.getFloat(R.styleable.CircularImage_CenterScaleX,
				(float) -1);
		centerScaleY = array.getFloat(R.styleable.CircularImage_CenterScaleY,
				(float) -1);
		centerRadius = array.getFloat(R.styleable.CircularImage_CenterRadius, 0);
		array.recycle();

		init();
		initPaint();
	}

	public void initPaint() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(false);
		borderPaint = new Paint();
		borderPaint.setAntiAlias(true);// 抗锯齿
		borderPaint.setStyle(Paint.Style.STROKE);// 设置空心圆
	}

	public void init() {
		if (centerScaleX == 0 || centerScaleY == 0) {
			// 动态添加控件
			centerScaleX = 0.5f;
			centerScaleY = 0.5f;
		}
		if(srcBitmap==null){
			return;
		}else{
			srcWidth = srcBitmap.getWidth();
			srcHeight = srcBitmap.getHeight();
		}
		
		// 未设置中心点，默认是图片的中心，半径是离边最短的距离
		if (centerScaleX == -1 || centerScaleY == -1) {
			centerScaleX = 0.5F;
			centerScaleY = 0.5F;
			radius = ((srcWidth < srcHeight) ? srcWidth : srcHeight) / 2;
		} else if (centerScaleX != -1 && centerScaleY != -1) {
			// 人为的指定了中心点，要先进行合理性判断
			if (centerScaleX <= 0 || centerScaleX >= 1 || centerScaleY <= 0 || centerScaleY >= 1) {
				throw new RuntimeException(
						"centerScaleX and centerScaleY must between 0 and 1");
			}
			if (centerRadius == 0) {
				// 距边界最短距离作为半径
				int w = (int) ((srcWidth * centerScaleX < (srcWidth - srcWidth
						* centerScaleX)) ? srcWidth * centerScaleX
						: (srcWidth - srcWidth * centerScaleX));
				int h = (int) ((srcHeight * centerScaleY < (srcHeight - srcHeight
						* centerScaleY)) ? srcHeight * centerScaleY
						: (srcHeight - srcHeight * centerScaleY));
				radius = (w < h) ? w : h;
			} else {
				// 人为设置了比例半径，将其与距边界最短距离比较后使用较小的值
				int w = (int) ((srcWidth * centerScaleX < (srcWidth - srcWidth
						* centerScaleX)) ? srcWidth * centerScaleX
						: (srcWidth - srcWidth * centerScaleX));
				int h = (int) ((srcHeight * centerScaleY < (srcHeight - srcHeight
						* centerScaleY)) ? srcHeight * centerScaleY
						: (srcHeight - srcHeight * centerScaleY));
				int min = (w < h) ? w : h;
				int r = (int) (centerRadius * ((srcWidth < srcHeight) ? srcWidth
						: srcHeight));
				radius = (min < r) ? min : r;
			}
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int resultWidth = 0;
		int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
		int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (modeWidth == MeasureSpec.EXACTLY) {
			resultWidth = sizeWidth;
		} else {
			if(srcBitmap!=null){
				// 如果为wrap_content，则比较圆形头像大小和父容器给的最大值sizeWidth
				int length = radius * 2;
				if (modeWidth == MeasureSpec.AT_MOST) {
					resultWidth = Math.min(sizeWidth, length);
				}
			}else{
				//动态添加
				resultWidth=sizeWidth;
			}
			
		}

		int resultHeight = 0;
		int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
		int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
		if (modeHeight == MeasureSpec.EXACTLY) {
			resultHeight = sizeHeight;
		} else {
			if(srcBitmap!=null){
				// 如果为wrap_content，则比较圆形头像大小和父容器给的最大值sizeHeight
				int length = radius * 2;
				if (modeHeight == MeasureSpec.AT_MOST) {
					resultHeight = Math.min(sizeHeight, length);
				}
			}else{
				//动态添加
				resultHeight=sizeHeight;
			}
		}

		setMeasuredDimension(resultWidth, resultHeight);
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
		Log.i(TAG, "width:" + width);
		Log.i(TAG, "height:" + height);
		create();
	}

	public void create() {
		// 在此处获得裁剪后的Bitmap
		dstBitmap = createDstBitmap();
		// 这里的scaleBitmap本该是圆形的，但可能在createScaledBitmap执行时被width,height拉伸变换
		if(dstBitmap!=null){
			scaleBitmap = Bitmap.createScaledBitmap(dstBitmap, width, height, true);
		}
	}

	/**
	 * 根据控件的大小裁剪Bitmap步骤： 根据圆心及半径裁剪圆形区域，生成新的Bitmap
	 * */
	public Bitmap createDstBitmap() {
		if(srcBitmap==null){
			return null;
		}
		int centerX = (int) (srcBitmap.getWidth() * centerScaleX);
		int centerY = (int) (srcBitmap.getHeight() * centerScaleY);
		dstBitmap = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		myCanvas = new Canvas(dstBitmap);

		// 获得画布后在Canvas进行裁剪
		int j = myCanvas.saveLayer(0, 0, radius * 2, radius * 2, null,
				Canvas.ALL_SAVE_FLAG);
		int x = centerX - radius;
		int y = centerY - radius;
		Bitmap bm = Bitmap
				.createBitmap(srcBitmap, x, y, radius * 2, radius * 2);
		myCanvas.drawBitmap(bm, 0, 0, mPaint);
		mPaint.setXfermode(modeInside);
		myCanvas.drawBitmap(createMask(), 0, 0, mPaint);
		mPaint.setXfermode(null);
		myCanvas.restoreToCount(j);

		if (betweenWidth != 0 || borderWidth != 0) {
			mPaint.setStyle(Paint.Style.STROKE);
			myCanvas.drawBitmap(createBorder(), 0, 0, mPaint);
		}
		return dstBitmap;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(scaleBitmap!=null){
			canvas.drawBitmap(scaleBitmap, 0, 0, null);
		}
	}

	// 通过内接矩形来创建圆形遮罩,
	public Bitmap createMask() {
		Bitmap bm = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		Paint paint = new Paint(1);
		paint.setColor(getResources().getColor(android.R.color.holo_blue_light));
		// 圆所内切的矩形
		RectF rectF = new RectF(0, 0, dstBitmap.getWidth(),
				dstBitmap.getHeight());
		canvas.drawArc(rectF, 0, 360, true, paint);

		return bm;
	}

	// 环形边界覆盖原图像的边界:分三个区域——内圆，环瓤和外圆,默认只绘制环瓤(从最外侧开始绘制)
	public Bitmap createBorder() {

		Bitmap bm = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		// 未设置内圆和外圆，只绘制环瓤，且该函数的被调用的前提是betweenWidth和borderWidth不全为0
		if (outBorderWidth == 0 && inBorderWidth == 0) {

			if (betweenWidth != 0 && borderWidth == 0) {
				borderWidth = betweenWidth;
			}
			// 使用borderWidth来画环，半径radius=realRadius-borderWidth/2
			borderPaint.setColor(borderColor);
			borderPaint.setStrokeWidth(borderWidth);
			canvas.drawCircle(radius, radius, radius - borderWidth / 2,
					borderPaint);
			return bm;
		}

		// 设置了内圆或外圆，且该函数的被调用的前提是betweenWidth和borderWidth不全为0
		if (outBorderWidth != 0 || inBorderWidth != 0) {

			// 内圆，环瓤和外圆都要绘制
			if (outBorderWidth != 0 && inBorderWidth != 0) {
				// 如果环瓤的宽度未设置，则用总宽度borderWidth减去外圆和内圆的宽度
				// 若设置了环瓤的宽度，那么总宽度borderWidth的限制就不会起作用了。
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - inBorderWidth - outBorderWidth;
				}
				// 外圆的绘制
				borderPaint.setColor(outBorderColor);
				borderPaint.setStrokeWidth(outBorderWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth / 2,
						borderPaint);
				// 环瓤的绘制
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth / 2, borderPaint);
				// 内圆的绘制
				borderPaint.setColor(inBorderColor);
				borderPaint.setStrokeWidth(inBorderWidth);// 宽为2
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth - inBorderWidth / 2, borderPaint);
				return bm;
			}

			// 只绘制环瓤和外圆
			if (outBorderWidth != 0 && inBorderWidth == 0) {
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - outBorderWidth;
				}
				// 外圆的绘制
				borderPaint.setColor(outBorderColor);
				borderPaint.setStrokeWidth(outBorderWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth / 2,
						borderPaint);
				// 环瓤的绘制
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth / 2, borderPaint);

				return bm;
			}
			// 只绘制环瓤和内圆
			if (outBorderWidth == 0 && inBorderWidth != 0) {
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - inBorderWidth;
				}
				// 环瓤的绘制
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - betweenWidth / 2,
						borderPaint);
				// 内圆的绘制
				borderPaint.setColor(inBorderColor);
				borderPaint.setStrokeWidth(inBorderWidth);// 宽为2
				canvas.drawCircle(radius, radius, radius - betweenWidth
						- inBorderWidth / 2, borderPaint);
				return bm;
			}
		}
		return null;
	}

	public void setImageBitmap(Bitmap bm){
		if(bm!=null){
			//重新测量和绘制
			srcBitmap=bm;
			init();
			super.requestLayout();
			invalidate();
		}
	}
	
}
