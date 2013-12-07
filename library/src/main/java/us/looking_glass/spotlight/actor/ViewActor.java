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

package us.looking_glass.spotlight.actor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.View;

import us.looking_glass.spotlight.Stage;

public class ViewActor implements Actor, View.OnLayoutChangeListener {
    private final static String TAG = ViewActor.class.getSimpleName();
    final static boolean debug = false;
    private final Context context;
    private boolean dirty = true;
    private Point center = new Point();
    private float radius = 0;
    private Stage host;
    private final View view;
    private final int spotlightPlacement;
    private final float spotlightSize;
    private final float innerPadding;
    public static final int AROUND = 1;
    public static final int INSIDE = 2;
    public static final int FIXED = 3;

    public ViewActor(Context context, View view, int placement, float size, float innerPadding) {
        this.context = context;
        this.view = view;
        spotlightPlacement = placement;
        spotlightSize = size;
        this.innerPadding = innerPadding;
    }

    private void update() {
        if (!dirty)
            return;
        int[] hostOffset = new int[2];
        int[] targetOffset = new int[2];
        host.getLocationOnScreen(hostOffset);
        view.getLocationOnScreen(targetOffset);
        int targetWidth = view.getMeasuredWidth();
        int targetHeight = view.getMeasuredHeight();
        int x = targetOffset[0] - hostOffset[0] + targetWidth / 2;
        int y = targetOffset[1] - hostOffset[1] + targetHeight / 2;
        float size = 1;
        switch (spotlightPlacement) {
            case AROUND:
                size = (float) Math.sqrt(targetHeight * targetHeight + targetWidth * targetWidth) / 2 + innerPadding / spotlightSize;
                break;
            case INSIDE:
                size = Math.min(targetWidth, targetHeight) / 2 - host.getSpotlight().getBorder() + innerPadding / spotlightSize;
                break;
            case FIXED:
                size = 1;
                break;
        }
        size *= spotlightSize;
        center.x = x;
        center.y = y;
        radius = size;
        Logv("Target size: %dx%d radius: %f", targetWidth, targetHeight, radius);
    }

    @Override
    public Point getPosition() {
        if (view == null || view.getVisibility() == View.GONE)
            return null;
        update();
        return center;
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public void show(Stage host) {
        this.host = host;
        if (view != null)
            view.addOnLayoutChangeListener(this);
    }

    @Override
    public void hide() {
        if (view != null)
            view.removeOnLayoutChangeListener(this);
        if (host != null)
            host = null;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        dirty = true;
        if (host != null)
            host.requestLayout();
    }
    
    public static class Builder {
        private final Context context;
        private View view = null;
        private int spotlightPlacement = AROUND;
        private float spotlightSize = 1;
        private final float defaultInnerPadding;
        private float innerPadding;

        public Builder(Context context) {
            this.context = context;
            defaultInnerPadding = context.getResources().getDisplayMetrics().density * 2;
            innerPadding = defaultInnerPadding;
        }

        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public Builder setView(int id) {
            this.view = ((Activity) context).findViewById(id);
            return this;
        }

        public Builder setPlacement(int spotlightPlacement) {
            this.spotlightPlacement = spotlightPlacement;
            return this;
        }

        public Builder setSize(float spotlightSize) {
            this.spotlightSize = spotlightSize;
            return this;
        }

        public void clear() {
            view = null;
            spotlightPlacement = AROUND;
            spotlightSize = 1;
            innerPadding = defaultInnerPadding;
        }

        public Actor build() {
            return new ViewActor(context, view, spotlightPlacement, spotlightSize, innerPadding);
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
