
package com.dapumptu.khtdoodle;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.EmbossMaskFilter;
import android.graphics.LightingColorFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class KHTSlowDoodleView extends View {

    public interface TouchEventListener {
        void onDoubleTap();
    }
    
    private static final String TAG = "KHTSlowDoodleView";
    //private static final float MINP = 0.25f;
    //private static final float MAXP = 0.75f;
    private static final float TOUCH_TOLERANCE = 4;

    private Paint mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;

    private Bitmap mBitmap;
    private Bitmap mBackgroundBitmap;
    private Bitmap mBrushBitmap;
    private Bitmap mMergedBitmap;
    
    private Paint mBitmapPaint;
    private Paint mBrushBitmapPaint;
    
    private Canvas mCanvas;
    private Path mPath;
    
    private RectF mCanvasBounds;
    private RectF mBackgroundBitmapRect;
    
    private float mX, mY;
    //private float mPathEffectPhase = 0;
    
    private boolean mSmoothDrawing = false;
    private boolean mUseBrush = false;
    private boolean mDrawBackground = true;
    //private final boolean mIsLongPressed = false;
    private final boolean mGradientDrawing = false;
    
    private TouchEventListener mTouchListener;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDoubleTapDetector;
    
    private float mScaleFactor;
    
    private Handler mHandler; 
    
    private Runnable mLongPressedTask = new Runnable() { 
        public void run() { 
            Log.i("", "Long press!");
            //mIsLongPressed = mIsLongPressed ? false : true;
        }   
    };
    
    public void setTouchListener(TouchEventListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }
    
    public boolean isUseBrush() {
        return mUseBrush;
    }
    
    public void setColor(int color) {
        mPaint.setColor(color);
        
        ColorFilter filter = new LightingColorFilter(color, color);
        mBrushBitmapPaint.setColorFilter(filter);
    }
    
    public int getColor() {
        return mPaint.getColor();
    }
    
    public void setBackgroundBitmap(Bitmap bitmap) {   
        if (mBackgroundBitmap != null)
        {
            //mBackgroundBitmap.recycle();
            mBackgroundBitmap = null;
        }
        
        if (bitmap != null) {
            mBackgroundBitmap = bitmap;
            mBackgroundBitmapRect.left = ((mCanvasBounds.width() - mBackgroundBitmap.getWidth()) / 2.0f);
            mBackgroundBitmapRect.top = ((mCanvasBounds.height() - mBackgroundBitmap.getHeight()) / 2.0f);
            mBackgroundBitmapRect.right = (mBackgroundBitmapRect.left + mBackgroundBitmap.getWidth());
            mBackgroundBitmapRect.bottom = (mBackgroundBitmapRect.top + mBackgroundBitmap.getHeight());
        }
        
        mDrawBackground = true;
        invalidate();
        
    }
    
    public Bitmap getBitmap() {
        Bitmap outputBitmap = Bitmap.createBitmap(mMergedBitmap);
        Canvas tempCanvas = new Canvas(outputBitmap);
        
        if (mBackgroundBitmap != null) {
            tempCanvas.drawBitmap(mBackgroundBitmap, mBackgroundBitmapRect.left, mBackgroundBitmapRect.top, mBitmapPaint);
        }
        
        tempCanvas.save();
        tempCanvas.scale(mScaleFactor, mScaleFactor);
        tempCanvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        tempCanvas.restore();
        
        tempCanvas.drawPath(mPath, mPaint);
        
        return outputBitmap;
    }
    
    public KHTSlowDoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public KHTSlowDoodleView(Context c) {
        super(c);
        init(c);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "View's onTouchEvent");
        
//        if (mDoubleTapDetector.onTouchEvent(event))
//            Log.d(TAG, "mDoubleTapDetector.onTouchEvent(event) is true");
//        else
//            Log.d(TAG, "mDoubleTapDetector.onTouchEvent(event) is false");
        
        if (mDoubleTapDetector.onTouchEvent(event)) {
            return true;
        } 
        
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //mIsLongPressed = false;
                //mHandler.postDelayed(mLongPressed, 2000);
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                //mHandler.removeCallbacks(mLongPressed);
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                //mHandler.removeCallbacks(mLongPressed);
                touchUp();
                invalidate();
                break;
        }
        //mScaleDetector.onTouchEvent(event);
        
        return true; 
        //return super.onTouchEvent(event);
    }
    
    public void reset() {
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xFF);
    }
    
    public void emboss() {
        if (mPaint.getMaskFilter() != mEmboss) {
            mPaint.setMaskFilter(mEmboss);
        } else {
            mPaint.setMaskFilter(null);
        }
    }
    
    public void blur() {
        if (mPaint.getMaskFilter() != mBlur) {
            mPaint.setMaskFilter(mBlur);
        } else {
            mPaint.setMaskFilter(null);
        }
    }
    
    public void erase() {
        mUseBrush = false;
        
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }
    
    public void srcATop() {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mPaint.setAlpha(0x80);
    }

    public void pathEffect() {
        PathEffect effect = new CornerPathEffect(40);
        mPaint.setPathEffect(effect);
    }

    public void smooth() {
        mSmoothDrawing = (mSmoothDrawing ? false : true);
    }

    public void gradient() {
        //mGradientDrawing = (mGradientDrawing ? false : true);
        
        if (!mGradientDrawing)
            mPaint.setShader(null);
        
        if (mGradientDrawing) {
            final int[] mColors = new int[] {
                    0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00,
                    0xFFFF0000
            };
            Shader s = new SweepGradient(mX, mY, mColors, null);
            mPaint.setShader(s);
        }
    }
    
    public void brush() {
        mUseBrush = (mUseBrush ? false : true);
    }
    
    public void clear() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mPath.reset();
        invalidate();
    }

    public void enableBackground() {
        mDrawBackground = (mDrawBackground ? false : true);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "onSizeChanged");
        
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mMergedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        //mCanvas.drawColor(0x00AAAAAA);
        mCanvasBounds.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawColor(0xFFAAAAAA);
        
        if (mDrawBackground && mBackgroundBitmap != null) {
            canvas.drawBitmap(mBackgroundBitmap, mBackgroundBitmapRect.left, mBackgroundBitmapRect.top, mBitmapPaint);
        }
        
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
        
        canvas.drawPath(mPath, mPaint);
    }
    
    private void init(Context c) {
        mScaleDetector = new ScaleGestureDetector(c, new ScaleListener());
        mScaleFactor = 1.0f;
        
        mDoubleTapDetector = new GestureDetector(c, new DoubleTapGestureListener());
        
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        
        mBrushBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mBrushBitmapPaint.setColor(0xFFFF0000);
        ColorFilter filter = new LightingColorFilter(0xFFFF0000, 0xFFFF0000);
        mBrushBitmapPaint.setColorFilter(filter);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mEmboss = new EmbossMaskFilter(new float[] {1, 1, 1}, 0.4f, 6, 3.5f);

        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        
        mHandler = new Handler();
        
        Resources res = c.getResources();
        // TODO: Put bitmap resource in drawable?
        mBrushBitmap = BitmapFactory.decodeResource(res, R.drawable.brush21);
        
        mCanvasBounds = new RectF();
        mBackgroundBitmapRect = new RectF();
    }

    private void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x / mScaleFactor, y / mScaleFactor);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        if (mUseBrush) {
            drawBrush(x, y);
            mX = x;
            mY = y;
        } else {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                if (mSmoothDrawing)
                    mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                else
                    mPath.lineTo(mX / mScaleFactor, mY / mScaleFactor);
                mX = x;
                mY = y;
            }
        }
    }
    
    private void touchUp() {

        if (mUseBrush) {
            ;
        } else {
            mPath.lineTo(mX / mScaleFactor, mY / mScaleFactor);

            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);

            // kill this so we don't double draw
            mPath.reset();
        }
    }
    
    private void drawBrush(float x, float y) {
        int halfBrushW = mBrushBitmap.getWidth() / 2;
        int halfBrushH = mBrushBitmap.getHeight() / 2;

        int distance = (int) distanceBetween2Points(x, y, mX, mY);
        double angle = angleBetween2Points(x, y, mX, mY);

        float xPos, yPos;
        double count = 1.0;
        for (int i = 0; (i <= distance || i == 0); i++) {
            xPos = (float) (mX + (Math.sin(angle) * count) - halfBrushW * 1.0);
            yPos = (float) (mY + (Math.cos(angle) * count) - halfBrushH * 1.0);
            mCanvas.drawBitmap(mBrushBitmap, xPos, yPos, mBrushBitmapPaint);
            
            count += 1.0;
        }
    }

    private float distanceBetween2Points(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);  
    }
    
    private float angleBetween2Points(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.atan2(dx, dy);
    }
    
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG, "Scale gesture detected");
            
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            invalidate();
            return true;
        }
    }
    
    private class DoubleTapGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "DoubleTapGestureListener";

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "View's double tap");
            mTouchListener.onDoubleTap();
            return super.onDoubleTap(e);
        }

    }
}
