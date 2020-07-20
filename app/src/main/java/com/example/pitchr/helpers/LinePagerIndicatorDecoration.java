package com.example.pitchr.helpers;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LinePagerIndicatorDecoration extends RecyclerView.ItemDecoration {

    private int colorActive = 0xFFFFFFFF;
    private int colorInactive = 0x66FFFFFF;

    private static final float DP = Resources.getSystem().getDisplayMetrics().density;

    private final int mIndicatorHeight = (int) (DP * 16);

    private final float mIndicatorItemLength = DP * 16;

    private final float mIndicatorItemPadding = DP * 4;

    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private final Paint mPaint = new Paint();

    public LinePagerIndicatorDecoration() {
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        float mIndicatorStrokeWidth = DP * 2;
        mPaint.setStrokeWidth(mIndicatorStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int itemCount = parent.getAdapter().getItemCount();

        // Center horizontally, calculate width and subtract half from center
        float totalLength = mIndicatorItemLength * itemCount;
        float paddingBetweenItems = Math.max(0, itemCount - 1) * mIndicatorItemPadding;
        float indicatorTotalWidth = totalLength + paddingBetweenItems;
        float indicatorStartX = (parent.getWidth() - indicatorTotalWidth) / 2F;

        // Center vertically in the allotted space
        float indicatorPosY = mIndicatorHeight * 1.5F;

        drawInactiveIndicators(c, indicatorStartX, indicatorPosY, itemCount);


        // Find active page (which should be highlighted)
        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        int activePosition = layoutManager.findFirstVisibleItemPosition();
        if (activePosition == RecyclerView.NO_POSITION) {
            return;
        }

        // Find offset of active page (if the user is scrolling)
        final View activeChild = layoutManager.findViewByPosition(activePosition);
        int left = activeChild.getLeft();
        int width = activeChild.getWidth();

        // On swipe the active item will be positioned from [-width, 0]
        // interpolate offset for smooth animation
        float progress = mInterpolator.getInterpolation(left * -1 / (float) width);

        drawHighlights(c, indicatorStartX, indicatorPosY, activePosition, progress, itemCount);
    }

    private void drawInactiveIndicators(Canvas c, float indicatorStartX, float indicatorPosY, int itemCount) {
        mPaint.setColor(colorInactive);

        // Width of item indicator including padding
        final float itemWidth = mIndicatorItemLength + mIndicatorItemPadding;

        float start = indicatorStartX;
        for (int i = 0; i < itemCount; i++) {
            // Draw the line for every item
            c.drawLine(start, indicatorPosY, start + mIndicatorItemLength, indicatorPosY, mPaint);
            start += itemWidth;
        }
    }

    private void drawHighlights(Canvas c, float indicatorStartX, float indicatorPosY,
                                int highlightPosition, float progress, int itemCount) {
        mPaint.setColor(colorActive);

        // Width of item indicator including padding
        final float itemWidth = mIndicatorItemLength + mIndicatorItemPadding;

        if (progress == 0F) {
            // No swipe, draw a normal indicator
            float highlightStart = indicatorStartX + itemWidth * highlightPosition;
            c.drawLine(highlightStart, indicatorPosY,
                    highlightStart + mIndicatorItemLength, indicatorPosY, mPaint);
        } else {
            float highlightStart = indicatorStartX + itemWidth * highlightPosition;
            // Calculate partial highlight
            float partialLength = mIndicatorItemLength * progress;

            // Draw the cut off highlight
            c.drawLine(highlightStart + partialLength, indicatorPosY,
                    highlightStart + mIndicatorItemLength, indicatorPosY, mPaint);

            // Draw the highlight overlapping to the next item as well
            if (highlightPosition < itemCount - 1) {
                highlightStart += itemWidth;
                c.drawLine(highlightStart, indicatorPosY,
                        highlightStart + partialLength, indicatorPosY, mPaint);
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // Offset the view at the top
        outRect.top = mIndicatorHeight;
    }
}