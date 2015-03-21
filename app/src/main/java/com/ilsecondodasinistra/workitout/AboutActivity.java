package com.ilsecondodasinistra.workitout;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView appNameTextView = (TextView) findViewById(R.id.app_name_and_version);
        appNameTextView.setText(getResources().getText(R.string.app_name) + " - " + pInfo.versionName);

        TextView emailTextView = (TextView) findViewById(R.id.authorEmailTV);
        emailTextView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView appInfoTextView = (TextView) findViewById(R.id.appInfoTV);
        appInfoTextView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView appAuthorTextView = (TextView) findViewById(R.id.appAuthorTV);
        appAuthorTextView.setMovementMethod(LinkMovementMethod.getInstance());

    }

}
