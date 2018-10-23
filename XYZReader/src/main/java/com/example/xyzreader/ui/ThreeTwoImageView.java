package com.example.xyzreader.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class ThreeTwoImageView extends AppCompatImageView {


    public ThreeTwoImageView(Context context) {
        super(context);
    }

    public ThreeTwoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreeTwoImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = width * 2/3;

        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY));
    }
}
