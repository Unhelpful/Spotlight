/**
 Copyright 2013 Andrew Mahone

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package us.looking_glass.spotlight.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;

public class RingSpotlight extends Spotlight {
    private Paint clearPaint;
    private Paint drawPaint;

    public RingSpotlight(Context context) {
        super(context);
        initPaints();
    }

    public RingSpotlight(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public RingSpotlight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setColor(0xffffffff);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        clearPaint.setStyle(Paint.Style.FILL);
        drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaint.setColor(getColor());
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(getBorder() * 2);
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        drawPaint.setColor(color);
    }

    @Override
    public void setBorder(float border) {
        super.setBorder(border);
        drawPaint.setStrokeWidth(border * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getRadius() == 0)
            return;
        int centerX = getOuterRadius();
        int centerY = getOuterRadius();
        canvas.drawCircle(centerX, centerY, getRadius(), drawPaint);
        canvas.drawCircle(centerX, centerY, getRadius(), clearPaint);
    }

    protected int getDefaultColor() {
        return 0xff33b5e5;
    }

    protected int getDefaultBorderWidth() {
        return 2;
    }
}
