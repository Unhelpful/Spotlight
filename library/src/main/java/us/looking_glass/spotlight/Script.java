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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.spec.OAEPParameterSpec;

import us.looking_glass.spotlight.actor.Actor;

public class Script implements View.OnClickListener {
    private final static String TAG = Script.class.getSimpleName();
    final static boolean debug = false;

    private final Activity activity;
    private List<Scene> scenes = new ArrayList<Scene>();
    private Iterator<Scene> sceneIterator = null;
    private ViewGroup frame = null;
    private Scene scene = null;
    private Stage stage = null;
    private Stage nextStage = null;
    private SharedPreferences sharedPreferences = null;
    private boolean showAll;

    public static final  int NONE = 0;
    public static final int FADE = 1;
    public static final int EMPTY_SCENE = 1<<31;

    public Script(Activity activity) {
        this.activity = activity;
    }
    
    public Scene add(Scene scene) {
        scenes.add(scene);
        return scene;
    }

    @Override
    public void onClick(View v) {
        scene.recordFired();
        stage.getButton().setOnClickListener(null);
        nextScene();
    }

    public void nextScene() {
        final Scene prevScene = scene;
        scene = null;
        while (sceneIterator.hasNext()) {
            scene = sceneIterator.next();
            if (showAll || scene.shouldDisplay())
                break;
            scene = null;
        }
        int transition = NONE;
        boolean end = scene == null;
        if (!end) {
            transition = scene.transition;
            if ((transition & EMPTY_SCENE) != 0) {
                transition &= ~EMPTY_SCENE;
                end = true;
            }
        }
        if (!end)
            setStage(false);
        else if (stage == null) {
            hide();
            return;
        }
        switch (transition) {
            case NONE:
                stage.show();
                if (!end) {
                    stage.setScene(scene);
                    stage.getButton().setOnClickListener(this);
                } else
                    hide();
                break;
            case FADE:
                AnimatorSet crossfade = new AnimatorSet();
                if (!end) {
                    if (prevScene != null) {
                        Logv("crossfade scenes");
                        setStage(true);
                        crossfade.playTogether(ObjectAnimator.ofFloat(stage, "alpha", 1, 0), ObjectAnimator.ofFloat(nextStage, "alpha", 0, 1));
                        nextStage.show();
                        nextStage.setScene(scene);
                        nextStage.setAlpha(0);
                    } else {
                        Logv("fade in first scene");
                        stage.show();
                        stage.setScene(scene);
                        stage.setAlpha(0);
                        crossfade.play(ObjectAnimator.ofFloat(stage, "alpha", 0, 1));
                    }
                } else {
                    Logv("fade out final scene");
                    crossfade.play(ObjectAnimator.ofFloat(stage, "alpha", 1, 0));
                }
                crossfade.setDuration(scene.animTime);
                crossfade.setStartDelay(0);
                final boolean finalEnd = end;
                crossfade.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!finalEnd) {
                            if (prevScene != null) {
                                Logv("swap scenes");
                                Stage tmpStage = nextStage;
                                stage.hide();
                                nextStage = stage;
                                stage = tmpStage;
                            }
                            Logv("%s %s", stage, stage != null ? stage.getButton() : null);
                            stage.getButton().setOnClickListener(Script.this);
                        } else
                            hide();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        onAnimationEnd(animation);
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }
                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                crossfade.start();
        }
    }

    public void setStage(boolean next) {
        if (frame == null) {
            frame = new FrameLayout(activity);
            ((ViewGroup) activity.getWindow().getDecorView()).addView(frame);
            Paint paint = null;
            int layerType = View.LAYER_TYPE_SOFTWARE;
            if (frame.isHardwareAccelerated()) {
                layerType = View.LAYER_TYPE_HARDWARE;
                paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
            }
            frame.setLayerType(layerType, paint);
        }
        if (stage == null) {
            stage = new Stage(activity);
            frame.addView(stage);
        }
        if (nextStage == null) {
            nextStage = new Stage(activity);
            stage.setLayerMode(PorterDuff.Mode.ADD);
            nextStage.setLayerMode(PorterDuff.Mode.ADD);
            frame.addView(nextStage);
        }
    }

    public void show() {
        show(false);
    }

    public void show(boolean showAll) {
        if (sceneIterator != null)
            return;
        this.showAll = showAll;
        sceneIterator = scenes.iterator();
        nextScene();
    }

    public void hide() {
        Logv("hide");
        if (stage != null)
            stage.setScene(null);
        if (nextStage != null)
            nextStage.setScene(null);
        if (frame != null)
            ((ViewGroup) activity.getWindow().getDecorView()).removeView(frame);
        sceneIterator = null;
        frame = null;
        stage = null;
        nextStage = null;
        scene = null;
    }

    public SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null)
            sharedPreferences = activity.getSharedPreferences("spotlight", Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    public class Scene {
        private final CharSequence titleText;
        private final CharSequence detailText;
        private final CharSequence buttonText;
        private final int buttonLayout;
        private final int spotlightLayout;
        private final int labelLayout;
        private final int transition;
        private final int animTime;
        private final Actor actor;
        private final int oneShotID;

        public Scene(CharSequence titleText, CharSequence detailText, CharSequence buttonText, int buttonLayout, int spotlightLayout, int labelLayout, Actor actor, int transition, int animTime, int oneShotID) {
            this.titleText = titleText;
            this.detailText = detailText;
            this.buttonText = buttonText;
            this.buttonLayout = buttonLayout;
            this.spotlightLayout = spotlightLayout;
            this.labelLayout = labelLayout;
            this.actor = actor;
            this.transition = transition;
            this.animTime = animTime;
            this.oneShotID = oneShotID;
        }

        public CharSequence getTitleText() {
            return titleText;
        }

        public CharSequence getDetailText() {
            return detailText;
        }

        public CharSequence getButtonText() {
            return buttonText;
        }

        public int getButtonLayout() {
            return buttonLayout;
        }

        public int getSpotlightLayout() {
            return spotlightLayout;
        }

        public int getLabelLayout() {
            return labelLayout;
        }

        public Actor getActor() {
            return actor;
        }

        private String prefsKey() {
            return String.format("oneShot%d", oneShotID);
        }

        private boolean shouldDisplay() {
            if (oneShotID < 0)
                return true;
            return getSharedPreferences().getBoolean(prefsKey(), true);
        }

        private void recordFired() {
            if (oneShotID < 0)
                return;
            getSharedPreferences().edit().putBoolean(prefsKey(), false).apply();
        }
    }

    public SceneBuilder getBuilder() {
        return new SceneBuilder();
    }

    public class SceneBuilder {
        private CharSequence defaultButtonText = null;
        private int defaultButtonLayout = -1;
        private int defaultSpotlightLayout = -1;
        private int defaultLabelLayout = -1;
        private int defaultTransition = NONE;
        private final int initialDefaultAnimTime = activity.getResources().getInteger(
                android.R.integer.config_mediumAnimTime);
        private int defaultAnimTime = initialDefaultAnimTime;
        private CharSequence titleText = null;
        private CharSequence detailText = null;
        private CharSequence buttonText = defaultButtonText;
        private int buttonLayout = defaultButtonLayout;
        private int spotlightLayout = defaultSpotlightLayout;
        private int labelLayout = defaultLabelLayout;
        private int transition = defaultTransition;
        private int animTime = defaultAnimTime;
        private Actor actor = null;
        private int oneShotID = -1;

        public SceneBuilder() {}
        
        public SceneBuilder setTitleText(int id) {
            titleText = activity.getText(id);
            return this;
        }
        
        public SceneBuilder setTitleText(CharSequence s) {
            titleText = s;
            return this;
        }

        public SceneBuilder setDetailText(int id) {
            detailText = activity.getText(id);
            return this;
        }

        public SceneBuilder setDetailText(CharSequence s) {
            detailText = s;
            return this;
        }
        public SceneBuilder setButtonText(int id) {
            titleText = activity.getText(id);
            return this;
        }

        public SceneBuilder clearDefaultButtonText() {
            return setDefaultButtonText(null);
        }

        public SceneBuilder setDefaultButtonText(int id) {
            return setDefaultButtonText(activity.getText(id));
        }

        public SceneBuilder setDefaultButtonText(CharSequence s) {
            defaultButtonText = s;
            buttonText = s;
            return this;
        }

        public SceneBuilder setButtonText(CharSequence s) {
            titleText = s;
            return this;
        }

        public SceneBuilder clearDefaultButtonLayout() {
            return setDefaultButtonLayout(-1);
        }

        public SceneBuilder setDefaultButtonLayout(int id) {
            defaultButtonLayout = id;
            buttonLayout = id;
            return this;
        }

        public SceneBuilder setButtonLayout(int id) {
            buttonLayout = id;
            return this;
        }

        public SceneBuilder clearDefaultSpotlightLayout() {
            return setDefaultSpotlightLayout(-1);
        }

        public SceneBuilder setDefaultSpotlightLayout(int id) {
            defaultSpotlightLayout = id;
            spotlightLayout = id;
            return this;
        }

        public SceneBuilder setSpotlightLayout(int id) {
            spotlightLayout = id;
            return this;
        }

        public SceneBuilder clearDefaultLabelLayout(int id) {
            return setDefaultLabelLayout(-1);
        }

        public SceneBuilder setDefaultLabelLayout(int id) {
            defaultLabelLayout = id;
            labelLayout = id;
            return this;
        }

        public SceneBuilder setLabelLayout(int id) {
            labelLayout = id;
            return this;
        }
        
        public SceneBuilder setActor(Actor actor) {
            this.actor = actor;
            return this;
        }

        public SceneBuilder clearDefaultTransition() {
            return setDefaultTransition(NONE);
        }

        public SceneBuilder setDefaultTransition(int transition) {
            defaultTransition = transition;
            this.transition = transition;
            return this;
        }

        public SceneBuilder setTransition(int transition) {
            this.transition = transition;
            return this;
        }

        public SceneBuilder clearDefaultAnimTime() {
            return setDefaultAnimTime(initialDefaultAnimTime);
        }

        public SceneBuilder setDefaultAnimTime(int milliseconds) {
            defaultAnimTime = milliseconds;
            animTime = milliseconds;
            return this;
        }

        public SceneBuilder setAnimTime(int milliseconds) {
            this.animTime = milliseconds;
            return this;
        }

        public SceneBuilder setOneShotID(int id) {
            oneShotID = id;
            return this;
        }

        public Scene build() {
            return new Scene(titleText, detailText, buttonText, buttonLayout, spotlightLayout, labelLayout, actor, transition, animTime, oneShotID);
        }
        
        public SceneBuilder clear() {
            titleText = null;
            detailText = null;
            buttonText = defaultButtonText;
            buttonLayout = defaultButtonLayout;
            spotlightLayout = defaultSpotlightLayout;
            labelLayout = defaultLabelLayout;
            actor = null;
            transition = defaultTransition;
            animTime = defaultAnimTime;
            oneShotID = -1;
            return this;
        }
        
        public SceneBuilder add() {
            Script.this.add(build());
            this.clear();
            return this;
        }

        public void end() {
            transition |= EMPTY_SCENE;
            add();
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
