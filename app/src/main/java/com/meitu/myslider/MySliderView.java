package com.meitu.myslider;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by meitu on 2017/8/17.
 */
public class MySliderView extends View {
    private static final String TAG = "Slider";

    /**
     * 画笔默认宽度
     */
    private static final int DEFAULT_PAINT_STROKE_WIDTH = 5;
    /**
     * 滑竿节点的默认填充颜色
     */
    private static final int DEFAULT_FILLED_COLOR = Color.parseColor("#FFFFA500");
    /**
     * 滑竿节点的默认颜色
     */
    private static final int DEFAULT_EMPTY_COLOR = Color.parseColor("#DDC3C3C3");
    /**
     * 滑竿的默认宽度
     */
    private static final float DEFAULT_BAR_HEIGHT_PERCENT = 0.10f;
    /**
     * 滑竿未选中点的半径
     */
    private static final float DEFAULT_SLOT_RADIUS_PERCENT = 0.125f;
    /**
     * 滑竿选中点的半径
     */
    private static final float DEFAULT_SLIDER_RADIUS_PERCENT = 0.25f;

    /**
     * 默认滑竿的节点数
     */
    private static final int DEFAULT_RANGE_COUNT = 5;

    /**
     * 控件的默认高度
     */
    private static final int DEFAULT_HEIGHT_IN_DP = 50;

    /**
     * 是否开启动画
     */
    private boolean isAnimate = false;

    protected Paint paint;

    protected Paint xFermodePaint;

    protected float radius;

    protected float slotRadius;

    private int currentIndex;

    private float currentSlidingX;

    private float currentSlidingY;

    private float selectedSlotX;

    private float selectedSlotY;

    private float animationStartX;

    private float animationEndX;

    private boolean gotSlot = false;

    private float[] slotPositions;

    private int filledColor = DEFAULT_FILLED_COLOR;

    private int emptyColor = DEFAULT_EMPTY_COLOR;

    private float barHeightPercent = DEFAULT_BAR_HEIGHT_PERCENT;

    private int rangeCount = DEFAULT_RANGE_COUNT;

    /**
     * 滑竿节点的间隔
     */
    private String rangeInterval = null;

    private int barHeight;

    private OnSlideListener listener;

    private float rippleRadius = 0.0f;

    private float downX;

    private float downY;

    private Path innerPath = new Path();

    private Path outerPath = new Path();

    private float slotRadiusPercent = DEFAULT_SLOT_RADIUS_PERCENT;

    private float sliderRadiusPercent = DEFAULT_SLIDER_RADIUS_PERCENT;

    /**
     * 控件的layoutHeight,layoutWidth
     */
    private int layoutHeight;

    private Xfermode xfermode;

    private Bitmap xfermodeBitmap;

    public MySliderView(Context context) {
        this(context, null);
    }

    public MySliderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MySliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView);
            TypedArray sa = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_height});
            try {
                layoutHeight = sa.getLayoutDimension(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT);
                rangeCount = a.getInt(
                        R.styleable.RangeSliderView_rangeCount, DEFAULT_RANGE_COUNT);
                filledColor = a.getColor(
                        R.styleable.RangeSliderView_filledColor, DEFAULT_FILLED_COLOR);
                emptyColor = a.getColor(
                        R.styleable.RangeSliderView_emptyColor, DEFAULT_EMPTY_COLOR);
                barHeightPercent = a.getFloat(
                        R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT);
                slotRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_slotRadiusPercent, DEFAULT_SLOT_RADIUS_PERCENT);
                sliderRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_sliderRadiusPercent, DEFAULT_SLIDER_RADIUS_PERCENT);
                rangeInterval = a.getString(
                        R.styleable.RangeSliderView_rangeInterval);
                isAnimate = a.getBoolean(
                        R.styleable.RangeSliderView_isAnimate, false);

            } finally {
                a.recycle();
                sa.recycle();
            }
        }

        setBarHeightPercent(barHeightPercent);
        setRangeCount(rangeCount);
        setRangeInterval(rangeInterval);
        setSlotRadiusPercent(slotRadiusPercent);
        setSliderRadiusPercent(sliderRadiusPercent);

        slotPositions = new float[rangeCount];
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(DEFAULT_PAINT_STROKE_WIDTH);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        xFermodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xFermodePaint.setStrokeWidth(DEFAULT_PAINT_STROKE_WIDTH);
        xFermodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        xFermodePaint.setAntiAlias(true);
        xFermodePaint.setFilterBitmap(true);
        xFermodePaint.setDither(true);

        xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
        xFermodePaint.setXfermode(xfermode);
        currentIndex = 0;
    }


    private void updateRadius(int height) {
        barHeight = (int) (height * barHeightPercent);
        radius = height * sliderRadiusPercent;
        slotRadius = height * slotRadiusPercent;
        Log.d(TAG, "updateRadius: ");
    }


    public int getRangeCount() {
        return rangeCount;
    }

    public void setRangeCount(int rangeCount) {
        if (rangeCount < 2) {
            throw new IllegalArgumentException("rangeCount must be >= 2");
        }
        this.rangeCount = rangeCount;
    }

    public void setRangeInterval(String rangeInterval) {

        this.rangeInterval = rangeInterval;
    }

    public float getBarHeightPercent() {
        return barHeightPercent;
    }

    public void setBarHeightPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Bar height percent must be in (0, 1]");
        }
        this.barHeightPercent = percent;
    }

    public float getSlotRadiusPercent() {
        return slotRadiusPercent;
    }

    public void setSlotRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slot radius percent must be in (0, 1]");
        }
        this.slotRadiusPercent = percent;
    }

    public float getSliderRadiusPercent() {
        return sliderRadiusPercent;
    }

    public void setSliderRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slider radius percent must be in (0, 1]");
        }
        this.sliderRadiusPercent = percent;
    }

    public void setOnSlideListener(OnSlideListener listener) {
        this.listener = listener;
    }

    /**
     * 在绘制之前计算位置，只计算一次
     */
    private void preComputeDrawingPosition() {
        int w = getWidthWithPadding();
        int h = getHeightWithPadding();

        int spacing = w / rangeCount;

        int y = getPaddingTop() + h / 2;
        currentSlidingY = y;
        selectedSlotY = y;

        int x;

        /** 判断是否设置点的不同间距*/
        if (rangeInterval == null) {
            /** 存储每个点的位置*/
            x = getPaddingLeft() + (spacing / 2);
            for (int i = 0; i < rangeCount; ++i) {
                slotPositions[i] = x;
                if (i == currentIndex) {
                    currentSlidingX = x;
                    selectedSlotX = x;
                }
                x += spacing;
            }
        } else {
            /** 存储每个点的位置 */
            String[] intervals = rangeInterval.split(",");
            setRangeCount(intervals.length + 1);
            int sumInterval = 0;
            w = (int) (w - radius * 2);
            for (int i = 0; i < intervals.length; i++) {
                sumInterval += Float.parseFloat(intervals[i]) * w;
                Log.d(TAG, "preComputeDrawingPosition: " + intervals[i] + " intervals " + Float.parseFloat(intervals[i]) * w);
            }
            x = (int) (getPaddingLeft() + (w - sumInterval) / 2 + radius);
            for (int i = 0; i < rangeCount; ++i) {
                slotPositions[i] = x;
                if (i == currentIndex) {
                    currentSlidingX = x;
                    selectedSlotX = x;
                }
                if (i < rangeCount - 1)
                    x += Float.parseFloat(intervals[i]) * w;
            }
        }
    }


    public void setInitialIndex(int index) {
        if (index < 0 || index >= rangeCount) {
            throw new IllegalArgumentException("Attempted to set index=" + index + " out of range [0," + rangeCount + "]");
        }
        currentIndex = index;
        currentSlidingX = selectedSlotX = slotPositions[currentIndex];
        invalidate();
    }

    public int getFilledColor() {
        return filledColor;
    }

    public void setFilledColor(int filledColor) {
        this.filledColor = filledColor;
        invalidate();
    }

    public int getEmptyColor() {
        return emptyColor;
    }

    public void setEmptyColor(int emptyColor) {
        this.emptyColor = emptyColor;
        invalidate();
    }

    public boolean isAnimate() {
        return isAnimate;
    }

    public void setAnimate(boolean animate) {
        isAnimate = animate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));

        // 得到高度后更新radius
        updateRadius(getHeight());

        // 计算点位置
        preComputeDrawingPosition();
        Log.d(TAG, "onMeasure: " + getMeasuredHeight());

    }

    /**
     * 根据measureSpec测量高度
     *
     * @param measureSpec
     * @return pixel 大小
     */
    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            final int height;
            if (layoutHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = dpToPx(getContext(), DEFAULT_HEIGHT_IN_DP);
            } else if (layoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                height = getMeasuredHeight();
            } else {
                height = layoutHeight;
            }
            result = height + getPaddingTop() + getPaddingBottom() + (2 * DEFAULT_PAINT_STROKE_WIDTH);
            //控件是高度wrap_content时s
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * 根据measureSpec测量高度
     *
     * @param measureSpec
     * @return pixel 大小
     */
    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = specSize + getPaddingLeft() + getPaddingRight() + (2 * DEFAULT_PAINT_STROKE_WIDTH) + (int) (2 * radius);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private void updateCurrentIndex() {
        float min = Float.MAX_VALUE;
        int j = 0;
        /** 找到slidingX距离最近的点*/
        for (int i = 0; i < rangeCount; ++i) {
            float dx = Math.abs(currentSlidingX - slotPositions[i]);
            if (dx < min) {
                min = dx;
                j = i;
            }
        }
        /** 监听器返回最终点索引*/
        if (j != currentIndex) {
            if (listener != null) {
                listener.onSlide(j);
            }
        }
        currentIndex = j;
        animationEndX = slotPositions[j];
        /** 动画效果 */
        if (isAnimate) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(animationStartX, animationEndX);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    currentSlidingX = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.setDuration((long) (Math.abs(animationEndX - animationStartX) * 3));
            valueAnimator.start();
        } else {
            currentSlidingX = slotPositions[j];
            //animateRipple();
            invalidate();
        }
        selectedSlotX = slotPositions[j];
        downX = currentSlidingX;
        downY = currentSlidingY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        float x = event.getX();
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gotSlot = isInSelectedSlot(x, y);
                downX = x;
                downY = y;
                Log.d(TAG, "onTouchEvent: startPosition " + slotPositions[0] + " end:" + slotPositions[rangeCount - 1]);
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: currentSlidingX:" + currentSlidingX);
                if (gotSlot) {
                    if (x < slotPositions[0]) {
                        currentSlidingX = slotPositions[0];
                    } else if (x > slotPositions[rangeCount - 1]) {
                        currentSlidingX = slotPositions[rangeCount - 1];
                    } else {
                        currentSlidingX = x;
                    }
                    currentSlidingY = y;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (gotSlot) {
                    gotSlot = false;
                    currentSlidingX = x;
                    if (x < slotPositions[0])
                        currentSlidingX = slotPositions[0];
                    if (x > slotPositions[rangeCount - 1])
                        currentSlidingX = slotPositions[rangeCount - 1];
                    currentSlidingY = y;
                    animationStartX = currentSlidingX;
                    updateCurrentIndex();
                }
                break;

        }
        return true;
    }

    private boolean isInSelectedSlot(float x, float y) {
        //只在滑竿节点生成滑动事件
      /*  return
                selectedSlotX - radius <= x && x <= selectedSlotX + radius &&
                        selectedSlotY - radius <= y && y <= selectedSlotY + radius;*/
        //在滑竿上生成滑动事件
        return slotPositions[0] - radius <= x && x <= slotPositions[rangeCount - 1] + radius &&
                selectedSlotY - radius <= y && y <= selectedSlotY + radius;
    }

    private void drawEmptySlots(Canvas canvas, int color) {
        paint.setColor(color);
        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            canvas.drawCircle(slotPositions[i], y, slotRadius, paint);
        }
    }

    public int getHeightWithPadding() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public int getWidthWithPadding() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private void drawFilledSlots(Canvas canvas, int color) {
        paint.setColor(color);
        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            if (slotPositions[i] <= currentSlidingX) {
                canvas.drawCircle(slotPositions[i], y, slotRadius, paint);
            }
        }
    }

    private void drawBar(Canvas canvas, float from, float to, int color) {
        paint.setColor(color);
        int h = getHeightWithPadding();
        int half = (barHeight >> 1);
        int y = getPaddingTop() + (h >> 1);
        canvas.drawRect(from, y - half, to, y + half, paint);
    }

 /*   private void drawRippleEffect(Canvas canvas) {
        if (rippleRadius != 0) {
            canvas.save();
            ripplePaint.setColor(Color.GRAY);
            outerPath.reset();
            outerPath.addCircle(downX, downY, rippleRadius, Path.Direction.CW);
            canvas.clipPath(outerPath);
            innerPath.reset();
            innerPath.addCircle(downX, downY, rippleRadius / 3, Path.Direction.CW);
            canvas.clipPath(innerPath, Region.Op.DIFFERENCE);
            canvas.drawCircle(downX, downY, rippleRadius, ripplePaint);
            canvas.restore();
        }
    }*/

    @Override
    public void onDraw(Canvas canvas) {
        int h = getHeightWithPadding();
        float border = slotPositions[0];
        float x0 = getPaddingLeft() + border;
        int y0 = getPaddingTop() + (h >> 1);

        int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.ALL_SAVE_FLAG);
        /** 绘制未选中圆形*/
        drawEmptySlots(canvas, getNoAlphaColor(emptyColor));
        /** 绘制空狭槽 */
        drawBar(canvas, slotPositions[0], slotPositions[rangeCount - 1], getNoAlphaColor(emptyColor));
        Log.d(TAG, "onDrawAlpha: " + isHasAlpha(filledColor));
        //判断填充小球是否有alpha
        if (isHasAlpha(filledColor)) {
            /** 绘制填充狭槽 */
            drawBar(canvas, x0, currentSlidingX, getNoAlphaColor(filledColor));
            /** 绘制填充圆形*/
            drawFilledSlots(canvas, getNoAlphaColor(filledColor));
            /** 绘制当前的圆形 */
            paint.setColor(getNoAlphaColor(filledColor));
            canvas.drawCircle(currentSlidingX, y0, radius, paint);
            //用Bitmap叠加效果
            canvas.drawBitmap(xfermodeBitmap, 0, 0, xFermodePaint);
            canvas.restoreToCount(save);
        } else {
            //用Bitmap叠加效果
            canvas.drawBitmap(xfermodeBitmap, 0, 0, xFermodePaint);
            canvas.restoreToCount(save);
            /** 绘制填充狭槽 */
            drawBar(canvas, x0, currentSlidingX, filledColor);
            /** 绘制填充圆形*/
            drawFilledSlots(canvas, filledColor);
            /** 绘制当前的圆形 */
            paint.setColor(filledColor);
            canvas.drawCircle(currentSlidingX, y0, radius, paint);
        }


        Log.d(TAG, "onDraw: " + getWidth() + " " + getHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: " + getHeight());
        if (xfermodeBitmap == null) {
            xfermodeBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(xfermodeBitmap);
            canvas.drawColor(getOnlyAlphaEmptyColor());
        }
    }

    private int getOnlyAlphaEmptyColor() {
        int alpha = Color.alpha(emptyColor);
        Log.d(TAG, "getOnlyAlphaEmptyColor: " + alpha);
        return Color.argb(alpha, 0, 0, 0);
    }

    private int getNoAlphaColor(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(255, red, green, blue);
    }

    private boolean isHasAlpha(int color) {
        int alpha = Color.alpha(color);
        Log.d(TAG, "isHasAlpha: " + alpha);
        if (alpha == 255)
            return false;
        else
            return true;

    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.saveIndex = this.currentIndex;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.currentIndex = ss.saveIndex;
    }


    static class SavedState extends BaseSavedState {
        int saveIndex;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.saveIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.saveIndex);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * pixel转化为Dp
     *
     * @param context
     * @param px
     * @return
     */
    static int pxToDp(final Context context, final float px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    /**
     * Dp转化为pixel
     *
     * @param context
     * @param dp
     * @return
     */
    static int dpToPx(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * 滑动位置的监听接口
     */
    public interface OnSlideListener {


        void onSlide(int index);
    }


}
