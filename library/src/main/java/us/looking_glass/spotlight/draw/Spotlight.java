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
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import us.looking_glass.spotlight.R;
import us.looking_glass.spotlight.Stage;
import us.looking_glass.spotlight.actor.Actor;

public abstract class Spotlight extends View {
    private int color;
    private float border;
    private float radius = 0;

    public Spotlight(Context context) {
        super(context);
        float metric = context.getResources().getDisplayMetrics().density;
        color = getDefaultColor();
        border = metric * getDefaultBorderWidth();
    }

    public Spotlight(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
    }

    public Spotlight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs);
    }

    private void initFromAttributes(Context context, AttributeSet attrs) {
        float metric = context.getResources().getDisplayMetrics().density;
        TypedArray styled = context.obtainStyledAttributes(attrs, R.styleable.Spotlight);
        color = styled.getColor(R.styleable.Spotlight_spotlightColor, getDefaultColor());
        border = styled.getDimension(R.styleable.Spotlight_spotlightBorderWidth, metric * getDefaultBorderWidth());
        styled.recycle();
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getOuterRadius() {
        return (int) Math.ceil(getRadius() + border);
    }

    public int getOuterDiameter() {
        return getOuterRadius() << 1;
    }

    public float getBorder() {
        return border;
    }

    public void setBorder(float border) {
        this.border = border;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        width = getOuterDiameter();
        height = getOuterDiameter();
        width = resolveSize(width, widthMeasureSpec);
        height = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    protected abstract int getDefaultColor();

    protected abstract int getDefaultBorderWidth();
}
