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

package us.looking_glass.spotlight.demo;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import us.looking_glass.spotlight.Script;
import us.looking_glass.spotlight.actor.ViewActor;

public class DemoActivity extends Activity implements View.OnClickListener {
    private static final String TAG = DemoActivity.class.getSimpleName();

    Script script;
    String versionText;
    final static String srcSample= "Script script = new Script(this);\nscript.getBuilder()\n    .setTitleText(R.string.spotlightIntroTitle)\n    .setDetailText(R.string.spotlightIntroDetail)\n    .add()\n    .setTitleText(R.string.spotlightFluentTitle)\n…";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        try {
            versionText = "v" + getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve package version", e);
            versionText = "vUnknown";
        }
        script = new Script(this);
        script.getBuilder()
                .setOneShotID(1)
                .setTitleText(R.string.spotlightIntroTitle)
                .setDetailText(R.string.spotlightIntroDetail)
                .add()
                .setOneShotID(2)
                .setTitleText(R.string.spotlightFluentTitle)
                .setDetailText(getString(R.string.spotlightFluentDetail, srcSample))
                .add()
                .setOneShotID(3)
                .setTitleText(R.string.spotlightTransitionsTitle)
                .setDetailText(R.string.spotlightTransitionsDetail)
                .setTransition(Script.FADE)
                .setAnimTime(2000)
                .add()
                .setOneShotID(4)
                .setTitleText(R.string.spotlightDefaultsTitle)
                .setDetailText(R.string.spotlightDefaultsDetail)
                .add()
                .setOneShotID(5)
                .setDefaultTransition(Script.FADE)
                .setLabelLayout(R.layout.demo_custom_label)
                .setTitleText(R.string.spotlightLayoutsTitle)
                .setDetailText(R.string.spotlightLayoutsDetail)
                .add()
                .setOneShotID(6)
                .setDetailText(R.string.spotlightCollapseDetail)
                .add()
                .setOneShotID(7)
                .setTitleText(R.string.spotlightKittenTitle)
                .setDetailText(R.string.spotlightKittenDetail)
                .setActor(new ViewActor.Builder(this)
                        .setView(R.id.demo_kitten)
                        .build())
                .add()
                .setOneShotID(8)
                .setTitleText(R.string.spotlightAroundBaconTitle)
                .setDetailText(R.string.spotlightAroundBaconDetail)
                .setActor(new ViewActor.Builder(this)
                        .setView(R.id.bacon)
                        .build())
                .add()
                .setOneShotID(9)
                .setTitleText(R.string.spotlightActorConfigurationTitle)
                .setDetailText(R.string.spotlightActorConfigurationDetail)
                .setActor(new ViewActor.Builder(this)
                        .setView(R.id.bacon)
                        .setPlacement(ViewActor.INSIDE)
                        .build())
                .add()
                .setOneShotID(10)
                .clearDefaultTransition()
                .setTitleText(R.string.spotlightSceneClearDefaultsTitle)
                .setDetailText(R.string.spotlightSceneClearDefaultsDetail)
                .add()
                .setTitleText(R.string.spotlightOneShotTitle)
                .setDetailText(R.string.spotlightOneShotDetail)
                .add();
        findViewById(R.id.repeatButton).setOnClickListener(this);
        script.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_about:
                openAboutPopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void openAboutPopup () {
        final Dialog aboutPopup = new Dialog(this);
        View aboutWindowView = getLayoutInflater().inflate(R.layout.about, null);
        TextView aboutTextView = (TextView) aboutWindowView.findViewById(R.id.aboutText);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
        TextView aboutVersionTextView = (TextView) aboutWindowView.findViewById(R.id.versionText);
        aboutVersionTextView.setText(versionText);
        aboutPopup.requestWindowFeature(Window.FEATURE_NO_TITLE);
        aboutPopup.setCancelable(true);
        aboutPopup.setCanceledOnTouchOutside(true);
        aboutPopup.setContentView(aboutWindowView);
        aboutPopup.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aboutPopup.show();
    }

    @Override
    public void onClick(View v) {
        script.show(true);
    }
}
