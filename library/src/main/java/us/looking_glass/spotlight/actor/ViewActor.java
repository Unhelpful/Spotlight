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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.View;

import java.sql.BatchUpdateException;

import us.looking_glass.spotlight.Stage;

public class ViewActor implements Actor {
    private final static String TAG = ViewActor.class.getSimpleName();
    final static boolean debug = true;
    private final Context context;
    private Point center = new Point();
    private float radius = 0;
    private Stage host;
    private final View view;
    private final int spotlightPlacement;
    private final float spotlightSize;
    private final float innerPadding;
    private boolean dirty = true;
    public static final int AROUND = 1;
    public static final int INSIDE = 2;
    public static final int FIXED = 3;
    Object listener = null;

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
                size = (float) Math.sqrt(targetHeight * targetHeight + targetWidth * targetWidth) / 2;
                size = size * spotlightSize + innerPadding;
                break;
            case INSIDE:
                size = Math.min(targetWidth, targetHeight) / 2 - host.getSpotlight().getBorder();
                size = size * spotlightSize - innerPadding;
                break;
            case FIXED:
                size = spotlightSize;
                break;
        }
        center.x = x;
        center.y = y;
        radius = size;
        Logv("Target size: %dx%d radius: %f", targetWidth, targetHeight, radius);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            dirty = false;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            addListener();
    }

    @Override
    public void hide() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            removeListener();
        if (host != null)
            host = null;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void addListener() {
        if (host == null || view == null)
            return;
        listener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Logv("layout changed");
                dirty = true;
                if (host != null)
                    host.forceLayout();
            }
        };
        view.addOnLayoutChangeListener((View.OnLayoutChangeListener) listener);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void removeListener() {
        if (host == null || view == null || listener == null)
            return;
        view.removeOnLayoutChangeListener((View.OnLayoutChangeListener) listener);
        listener = null;
    }
}
