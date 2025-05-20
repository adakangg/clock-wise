package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

/**
 * This activity displays attributions and credits for images and graphics used within this application.
 */

public class AttributionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributions);
        setupToolbar();
        setGraphic();
        listAttributions();
    }

    /**
     * @noinspection deprecation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_attributions);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "PrivateResource"})
    private void listAttributions() {
        String[] attributions = getResources().getStringArray(R.array.image_attributions);
        LinearLayout listLayout = findViewById(R.id.layout_attributions);

        for (String attribution : attributions) {
            LinearLayout attributionLayout = new LinearLayout(this);
            attributionLayout.setOrientation(LinearLayout.HORIZONTAL);

            ImageView imgBulletPoint = new ImageView(this);
            imgBulletPoint.setImageResource(R.drawable.triangle);
            imgBulletPoint.setRotation(-90);
            imgBulletPoint.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            TextView tvAttributionName = new TextView(this);
            tvAttributionName.setText(attribution);
            tvAttributionName.setTextSize(16);
            Typeface typeface = ResourcesCompat.getFont(this, R.font.poppins_medium);
            tvAttributionName.setTypeface(typeface);

            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
            int textColor = typedValue.data;
            tvAttributionName.setTextColor(textColor);
            tvAttributionName.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            attributionLayout.addView(imgBulletPoint);
            attributionLayout.addView(tvAttributionName);
            attributionLayout.setPadding(0, 15, 0, 15);
            listLayout.addView(attributionLayout);
        }
    }

    private void setGraphic() {
        ImageView imgPainting = findViewById(R.id.img_painting);
        int currentTheme = AppCompatDelegate.getDefaultNightMode();
        boolean isDarkTheme = currentTheme == AppCompatDelegate.MODE_NIGHT_YES;
        imgPainting.setImageResource(isDarkTheme ? R.drawable.painter_dark : R.drawable.painter_light);
    }
}