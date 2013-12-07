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

package us.looking_glass.spotlight;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import us.looking_glass.spotlight.draw.Spotlight;

public class Stage extends ViewGroup implements View.OnTouchListener {
    private final static String TAG = Stage.class.getSimpleName();
    final static boolean debug = false;
    
    private int spotlightLayout = R.layout.default_spotlight;
    private int labelLayout = R.layout.default_label;
    private int buttonLayout = R.layout.default_button;
    private int curSpotlightLayout = -1;
    private int curLabelLayout = -1;
    private int curButtonLayout = -1;
    private CharSequence origButtonText;


    public Spotlight getSpotlight() {
        return spotlight;
    }

    private Spotlight spotlight = null;
    private View label = null;
    private Button button = null;
    private Script.Scene scene;

    public static Stage install(Activity activity, Stage stage) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        decorView.addView(stage, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        return stage;
    }

    public static Stage install(Activity activity, int id) {
        Stage stage = (Stage) activity.getLayoutInflater().inflate(id, null);
        return install(activity, stage);
    }

    public static Stage install(Activity activity) {
        Stage stage = new Stage(activity);
        return install(activity, stage);
    }

    public void remove() {
        Activity activity = (Activity) getContext();
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        decorView.removeView(this);
    }

    public Stage(Context context) {
        this(context, null, R.styleable.AppTheme_stageStyle);
    }

    public Stage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray styled = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Stage, R.attr.stageStyle, R.style.Stage);
        setBackgroundColor(styled.getColor(R.styleable.Stage_stageBackgroundColor, 0xc0000000));
        spotlightLayout = styled.getResourceId(R.styleable.Stage_stageSpotlightLayout, R.layout.default_spotlight);
        labelLayout = styled.getResourceId(R.styleable.Stage_stageLabelLayout, R.layout.default_label);
        buttonLayout = styled.getResourceId(R.styleable.Stage_stageButtonLayout, R.layout.default_button);
        styled.recycle();
        setVisibility(GONE);
        String packageName = context.getPackageName();
        Resources resources = context.getResources();
    }

    public Button getButton() {
        return button;
    }

    void setLayerMode(PorterDuff.Mode mode) {
        Paint layerPaint = null;
        if (mode != null) {
            layerPaint = new Paint();
            layerPaint.setXfermode(new PorterDuffXfermode(mode));
        }
        if (isHardwareAccelerated()) {
            setLayerType(LAYER_TYPE_HARDWARE, layerPaint);
        } else {
            setLayerType(LAYER_TYPE_SOFTWARE, layerPaint);
        }
    }

    private int getVisibleTop() {
        TypedValue tv = new TypedValue();
        Rect visible = new Rect();
        ((Activity) getContext()).getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
        int result = visible.top;
        if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            result += TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            layoutFromParams(getChildAt(i));
        }
    }

    private final void layoutFromParams(View v) {
        LayoutParams lps = (LayoutParams) v.getLayoutParams();
        v.layout(lps.left, lps.top, lps.right, lps.bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Logv("onMeasure");
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int myWidth = resolveSize(displayMetrics.widthPixels, widthMeasureSpec);
        int myHeight = resolveSize(displayMetrics.heightPixels, heightMeasureSpec);
        setMeasuredDimension(myWidth, myHeight);
        myWidth = getMeasuredWidth();
        myHeight = getMeasuredHeight();
        Logv("measured dimensions: %dx%d", myWidth, myHeight);
        int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        button.measure(unspecified, unspecified);
        LayoutParams buttonParams = (LayoutParams) button.getLayoutParams();
        LayoutParams spotlightParams = (LayoutParams) spotlight.getLayoutParams();
        LayoutParams labelParams = (LayoutParams) label.getLayoutParams();
        int buttonWidth = button.getMeasuredWidth();
        int buttonHeight = button.getMeasuredHeight();
        buttonParams.right = myWidth;
        buttonParams.bottom = myHeight;
        buttonParams.left = buttonParams.right - buttonWidth - buttonParams.rightMargin - buttonParams.leftMargin;
        buttonParams.top = buttonParams.bottom - buttonHeight - buttonParams.bottomMargin - buttonParams.topMargin;
        spotlightParams.left = spotlightParams.right = myWidth;
        spotlightParams.top = spotlightParams.bottom = myHeight;
        Point spotlightPosition = null;
        if (scene != null && scene.getActor() != null)
            spotlightPosition = scene.getActor().getPosition();
        Logv("button margins: %d %d %d %d", buttonParams.leftMargin, buttonParams.topMargin, buttonParams.rightMargin, buttonParams.bottomMargin    );
        int topSpace = getVisibleTop();
        if (spotlightPosition == null) {
            Logv("no spotlight");
            spotlight.setVisibility(GONE);
            spotlight.setRadius(0);
            spotlight.measure(unspecified, unspecified);
        } else {
            Logv("spotlight");
            spotlight.setVisibility(VISIBLE);
            spotlight.setRadius(scene.getActor().getRadius());
            spotlight.measure(unspecified, unspecified);
            int spotlightRadius = spotlight.getOuterRadius();
            Logv("spotlight position: %d,%d diameter: %d, measured dimensions: %dx%d", spotlightPosition.x, spotlightPosition.y, spotlight.getOuterDiameter(), spotlight.getMeasuredWidth(), spotlight.getMeasuredHeight());
            spotlightParams.left = spotlightPosition.x - spotlightRadius - spotlightParams.leftMargin;
            spotlightParams.right = spotlightPosition.x + spotlightRadius + spotlightParams.rightMargin;
            spotlightParams.top = spotlightPosition.y - spotlightRadius - spotlightParams.topMargin;
            spotlightParams.bottom = spotlightPosition.y + spotlightRadius + spotlightParams.bottomMargin;
        }

        int labelHeight = 0;
        int labelHorizontalMargins = labelParams.leftMargin + labelParams.rightMargin;
        int labelVerticalMargins = labelParams.topMargin + labelParams.bottomMargin;
        boolean gotLabel = true;
        do {
            int v;
            labelHeight = measureLabel(myWidth - labelHorizontalMargins);
            v = Math.min(spotlightParams.top, buttonParams.top);
            if (labelHeight <= v - topSpace - labelVerticalMargins) {
                Logv("placed label above Spotlight");
                labelParams.left = labelParams.leftMargin;
                labelParams.top = topSpace + labelParams.topMargin;
                labelParams.right = labelParams.left + label.getMeasuredWidth();
                labelParams.bottom = labelParams.top + labelHeight;
                break;
            } else
                Logv("not enough space above Spotlight: %d", v - topSpace - labelVerticalMargins);
            v = (buttonParams.top > spotlightParams.bottom ? buttonParams.top : myHeight) - spotlightParams.bottom;
            if (labelHeight <= v - labelVerticalMargins) {
                Logv("placed label below Spotlight");
                labelParams.left = labelParams.leftMargin;
                labelParams.top = spotlightParams.bottom + labelParams.topMargin;
                labelParams.right = labelParams.left + label.getMeasuredWidth();
                labelParams.bottom = labelParams.top + labelHeight;
                break;
            } else
                Logv("not enough space below Spotlight: %d", v - labelVerticalMargins);
            labelHeight = measureLabel(spotlightParams.left - labelHorizontalMargins);
            v = buttonParams.left < spotlightParams.left ? buttonParams.top : myHeight;
            if (labelHeight <= v - topSpace - labelVerticalMargins) {
                Logv("placed label left of Spotlight");
                labelParams.left = labelParams.leftMargin;
                labelParams.top = topSpace + labelParams.topMargin;
                labelParams.right = labelParams.left + label.getMeasuredWidth();
                labelParams.bottom = labelParams.top + labelHeight;
                break;
            } else
                Logv("not enough space left of Spotlight: %d", v - topSpace - labelVerticalMargins);
            labelHeight = measureLabel(myWidth - spotlightParams.right - labelHorizontalMargins);
            v = buttonParams.right > spotlightParams.right ? buttonParams.top : myHeight;
            if (labelHeight <= v - topSpace - labelVerticalMargins) {
                Logv("placed label right of Spotlight");
                labelParams.left = spotlightParams.right + labelParams.leftMargin;
                labelParams.top = topSpace + labelParams.topMargin;
                labelParams.right = labelParams.left + label.getMeasuredWidth();
                labelParams.bottom = labelParams.top + labelHeight;
                break;
            } else
                Logv("not enought space right of Spotlight: %d", v - topSpace - labelVerticalMargins);
            labelHeight = measureLabel(buttonParams.left - labelHorizontalMargins);
            v = spotlightParams.bottom < buttonParams.top ? spotlightParams.bottom : topSpace;
            if (labelHeight <= myHeight - v - labelVerticalMargins) {
                Logv("placed label left of button");
                labelParams.left = labelParams.leftMargin;
                labelParams.top = v + labelParams.topMargin;
                labelParams.right = labelParams.left + label.getMeasuredWidth();
                labelParams.bottom = labelParams.top + labelHeight;
                break;
            } else
                Logv("not enought space left of button: %d", myHeight - v - labelVerticalMargins);
            Logv("label fit failed, using fallback fullscreen placement");
            labelParams.left = labelParams.leftMargin;
            labelParams.top = topSpace + labelParams.topMargin;
            labelParams.right = myWidth - labelParams.rightMargin;
            labelParams.bottom = myHeight - labelParams.bottomMargin;
            int width = MeasureSpec.makeMeasureSpec(labelParams.right - labelParams.left, MeasureSpec.AT_MOST);
            int height = MeasureSpec.makeMeasureSpec(labelParams.bottom - labelParams.top, MeasureSpec.AT_MOST);
            label.measure(width, height);
        } while (false);

        spotlightParams.left += spotlightParams.leftMargin;
        spotlightParams.top += spotlightParams.topMargin;
        spotlightParams.right -= spotlightParams.rightMargin;
        spotlightParams.bottom -= spotlightParams.bottomMargin;

        buttonParams.left += buttonParams.leftMargin;
        buttonParams.top += buttonParams.bottomMargin;
        buttonParams.right -= buttonParams.rightMargin;
        buttonParams.bottom -= buttonParams.bottomMargin;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view == spotlight || view == label || view == button)
                continue;
            else
                view.setVisibility(GONE);
        }
    }

    private int measureLabel(int width) {
        if (width < 1)
            return Integer.MAX_VALUE;
        width = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
        int height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        label.measure(width, height);
        Logv("measureLabel: %dx%d", label.getMeasuredWidth(), label.getMeasuredHeight());
        return label.getMeasuredHeight();
    }

    void updateChildViews() {
        int nextLayout = scene == null ? -1 : scene.getSpotlightLayout();
        if (nextLayout == -1)
            nextLayout = spotlightLayout;
        spotlight = (Spotlight) updateChildView(0, spotlight, curSpotlightLayout, nextLayout);
        curSpotlightLayout = nextLayout;

        nextLayout = scene == null ? -1 : scene.getLabelLayout();
        if (nextLayout == -1)
            nextLayout = labelLayout;
        label = updateChildView(1, label, curLabelLayout, nextLayout);
        curLabelLayout = nextLayout;

        nextLayout = scene == null ? -1 : scene.getButtonLayout();
        if (nextLayout == -1)
            nextLayout = buttonLayout;
        button = (Button) updateChildView(2, button, curButtonLayout, nextLayout);
        if (curButtonLayout != nextLayout)
            origButtonText = button.getText();
        curButtonLayout = nextLayout;
        if (scene != null) {
            CharSequence buttonText = scene.getButtonText();
            if (buttonText != null)
                button.setText(buttonText);
            CharSequence titleText = scene.getTitleText();
            CharSequence detailText = scene.getDetailText();
            int labelVisibility = GONE;
            TextView titleTextView = (TextView) label.findViewById(R.id.spotlightTitleText);
            if (titleTextView != null) {
                if (titleText != null) {
                    titleTextView.setText(titleText);
                    titleTextView.setVisibility(VISIBLE);
                    labelVisibility = VISIBLE;
                } else
                    titleTextView.setVisibility(GONE);
            }
            TextView detailTextView = (TextView) label.findViewById(R.id.spotlightDetailText);
            if (detailTextView != null) {
                if (detailText != null) {
                detailTextView.setText(detailText);
                detailTextView.setVisibility(VISIBLE);
                labelVisibility = VISIBLE;
                } else
                    detailTextView.setVisibility(GONE);
            }
            label.setVisibility(labelVisibility);
            button.setVisibility(VISIBLE);
        }
    }

    View updateChildView(int index, View prev, int prevID, int nextID) {
        if (nextID == prevID)
            return prev;
        View next = LayoutInflater.from(getContext()).inflate(nextID, this, false);
        removeView(prev);
        addView(next, index);
        return next;
    }

    public void show() {
        setVisibility(VISIBLE);
        updateChildViews();
        if (scene != null && scene.getActor() != null)
            scene.getActor().show(this);
        setOnTouchListener(this);
        requestLayout();
    }

    public void hide() {
        setVisibility(GONE);
        if (scene != null && scene.getActor() != null)
            scene.getActor().hide();
        setOnTouchListener(null);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(getContext(), null);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public void setScene(Script.Scene scene) {
        Script.Scene prev = this.scene;
        this.scene = scene;
        if (getVisibility() == VISIBLE) {
            if (prev != null && prev.getActor() != null) {
                prev.getActor().hide();
            }
            updateChildViews();
            if (scene != null && scene.getActor() != null)
                scene.getActor().show(this);
            requestLayout();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    public class LayoutParams extends MarginLayoutParams {
        private int top = 0;
        private int bottom = 0;
        private int left = 0;
        private int right = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private static final void Logd(String text, Object... args) {
        if (debug) {
            if (args != null && args.length > 0)
                text = String.format(text, args);
            Log.d(TAG, text);
        }
    }

    private static final void Logv(String text, Object... args) {
        if (debug) {
            if (args != null && args.length > 0)
                text = String.format(text, args);
            Log.v(TAG, text);
        }
    }
}
