package com.spazedog.lib.utilsLib.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.spazedog.lib.utilsLib.R;
import com.spazedog.lib.utilsLib.utils.Conversion;

public class ExtendedFrameLayout extends FrameLayout implements ExtendedView {

    protected Integer mMaxWidth = 0;
    protected Integer mMaxHeight = 0;
    protected Integer mMinWidth = 0;
    protected Integer mMinHeight = 0;

    protected Boolean mAdoptWidth = false;
    protected Boolean mAdoptHeight = false;

    public ExtendedFrameLayout(Context context) {
        super(context);
    }

    public ExtendedFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewDimensionOptions);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.ViewDimensionOptions_layout_dimens_maxWidth, 0);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.ViewDimensionOptions_layout_dimens_maxHeight, 0);
        mMinWidth = a.getDimensionPixelSize(R.styleable.ViewDimensionOptions_layout_dimens_minWidth, 0);
        mMinHeight = a.getDimensionPixelSize(R.styleable.ViewDimensionOptions_layout_dimens_minHeight, 0);

        mAdoptWidth = a.getBoolean(R.styleable.ViewDimensionOptions_layout_dimens_setHeightAsWidth, false);
        mAdoptHeight = a.getBoolean(R.styleable.ViewDimensionOptions_layout_dimens_setWidthAsHeight, false);
        a.recycle();

        setupShadow(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int maxWidth = mMaxWidth;
        int maxHeight = mMaxHeight;
        int minWidth = mMinWidth;
        int minHeight = mMinHeight;

        if (getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) getParent();
            int paddingVertical = parent.getPaddingTop() + parent.getPaddingBottom();
            int paddingHorizontal = parent.getPaddingLeft() + parent.getPaddingRight();

            maxWidth -= paddingHorizontal;
            maxHeight -= paddingVertical;
            minWidth -= paddingHorizontal;
            minHeight -= paddingVertical;
        }

        if (mAdoptWidth && width != height) {
            width = height;
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
        }

        if (mAdoptWidth && width != height) {
            height = width;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(widthMeasureSpec));
        }

        if ((maxWidth > 0 && width > maxWidth) || (minWidth > 0 && width < minWidth)) {
            width = maxWidth > 0 && width > maxWidth ? maxWidth : minWidth;
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
        }

        if ((maxHeight > 0 && height > maxHeight) || (minHeight > 0 && height < minHeight)) {
            height = maxHeight > 0 && height > maxHeight ? maxHeight : minHeight;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec));
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @SuppressLint("NewApi")
    private void triggerRequestLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!isInLayout()) {
                requestLayout();
            }

        } else {
            requestLayout();
        }
    }

    public void adoptWidth(boolean adopt) {
        mAdoptWidth = adopt;
        triggerRequestLayout();
    }

    public void adoptHeight(boolean adopt) {
        mAdoptHeight = adopt;
        triggerRequestLayout();
    }

    public void setMinHeight(int min) {
        mMinHeight = min;
        triggerRequestLayout();
    }

    public void setMinWidth(int min) {
        mMinWidth = min;
        triggerRequestLayout();
    }

    public void setMaxHeight(int max) {
        mMaxHeight = max;
        triggerRequestLayout();
    }

    public void setMaxWidth(int max) {
        mMaxWidth = max;
        triggerRequestLayout();
    }

    public void setTotalHeight(int total) {
        mMinHeight = total;
        mMaxHeight = total;
        triggerRequestLayout();
    }

    public void setTotalWidth(int total) {
        mMinWidth = total;
        mMaxWidth = total;
        triggerRequestLayout();
    }


	/*
	 * =====================================================================
	 * ---------------------------------------------------------------------
	 *
	 * 		Shadow Drawing
	 */

    private Drawable mShadowDrawable;
    private NinePatchDrawable mShadowNinePatchDrawable;
    private int mShadowOffset;
    private boolean mShadowVisible;
    private int mWidth, mHeight;
    private float mAlpha = 1f;

    @SuppressLint("NewApi")
    private void setupShadow(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewShadowOptions);
        mShadowVisible = a.getBoolean(R.styleable.ViewShadowOptions_layout_setShadow, false);
        mShadowOffset = a.getInt(R.styleable.ViewShadowOptions_layout_setShadowOffset, 1);
        a.recycle();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mShadowDrawable = context.getResources().getDrawable(R.drawable.bottom_shadow);
            if (mShadowDrawable != null) {
                mShadowDrawable.setCallback(this);
                if (mShadowDrawable instanceof NinePatchDrawable) {
                    mShadowNinePatchDrawable = (NinePatchDrawable) mShadowDrawable;
                }
            }

            setWillNotDraw(!mShadowVisible || mShadowDrawable == null);

        } else {
            setElevation(Conversion.dipToPixels(mShadowVisible ? Conversion.dipToPixels(mShadowOffset) : 0));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        updateShadowBounds();
    }

    private void updateShadowBounds() {
        if (mShadowDrawable != null) {
            mShadowDrawable.setBounds(0, mHeight-Conversion.dipToPixels(1), mWidth, mHeight);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mShadowDrawable != null && mShadowVisible) {
            if (mShadowNinePatchDrawable != null) {
                mShadowNinePatchDrawable.getPaint().setAlpha((int) (255 * mAlpha));
            }
            mShadowDrawable.draw(canvas);
        }
    }

    @SuppressLint("NewApi")
    public void setShadowOffset(int shadowTopOffset) {
        mShadowOffset = shadowTopOffset;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            updateShadowBounds();
            triggerRequestLayout();

        } else {
            setElevation(Conversion.dipToPixels(mShadowVisible ? Conversion.dipToPixels(mShadowOffset) : 0));
        }
    }

    @SuppressLint("NewApi")
    public void setShadowVisible(boolean shadowVisible) {
        mShadowVisible = shadowVisible;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setWillNotDraw(!mShadowVisible || mShadowDrawable == null);
            triggerRequestLayout();

        } else {
            setElevation(Conversion.dipToPixels(mShadowVisible ? Conversion.dipToPixels(mShadowOffset) : 0));
        }
    }
}
