package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;

import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.TileSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.alarmclock.utils.Util;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** @noinspection deprecation
 *  This fragment hosts an interactive map using MapLibre where users can view geolocation data retrieved from the GeoNames API.
 *  It also provides a timezone converter, enabling users to convert date/time values from one timezone to another.
 */
public class ConverterFragment extends Fragment {
    private CustomMapView mapView;
    private MapLibreMap libreMap;
    private Marker marker;
    private ViewPager2 viewPager;
    private ConstraintLayout layoutMap;
    private LinearLayout layoutEndZdt;
    private ProgressBar progressBar;
    private TextView tvMapZoneDesc, tvMapLocationDesc,  tvStartTime, tvStartDate, dateTitle, tvEndTime, tvEndDate;
    private EditText hrInput, minInput;
    private Spinner spnEndZones;
    Map<String, String> zonesMap;
    private ZonedDateTime zdtStart;
    private ZoneId zoneIdStart, zoneIdEnd;
    private ToggleButton amPmBtn;
    private ScrollView scrollView;
    private long touchStartTime;
    private final int MAX_CLICK_DURATION = 200;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_converter, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewPager = requireActivity().findViewById(R.id.viewpager_alarms);
        zdtStart = ZonedDateTime.now();
        zonesMap = Util.getTimeZoneMap();
        scrollView = view.findViewById( R.id.scrollview_converter);
        scrollView.setSmoothScrollingEnabled(true);
        setUpMap(view, savedInstanceState);
        setupZoneSpinners(view);
        setupStartZdtPicker(view);
        setupEndZdtPicker(view);
        Button btnConvert = view.findViewById(R.id.btn_convert);
        btnConvert.setOnClickListener(v -> convertZdt());
    }


    /* prevents scroll view from jumping to top */
    private void maintainScrollPosition() {
        int scrollY = scrollView.getScrollY();
        scrollView.post(() -> scrollView.smoothScrollTo(0, scrollY));
    }

    private void setUpMap(View view, Bundle savedInstanceState) {
        layoutMap = view.findViewById(R.id.layout_map);
        layoutMap.setVisibility(View.GONE);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        tvMapZoneDesc = view.findViewById(R.id.text_map_zone);
        tvMapLocationDesc = view.findViewById(R.id.text_map_location);
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        setupMapView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMapView() {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(0.0, 0.0)).zoom(1.0).build();
        mapView.getMapAsync(map -> {
            libreMap = map;
            String osmTileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
            libreMap.setStyle(new Style.Builder().fromUrl("https://demotiles.maplibre.org/style.json")
                    .withSource(new RasterSource("osm-source",
                            new TileSet("tileset", osmTileUrl),
                            256))
                    .withLayer(new RasterLayer("osm-layer", "osm-source")));
            map.setCameraPosition(cameraPosition);
            map.getUiSettings().setZoomGesturesEnabled(true);
            map.getUiSettings().setScrollGesturesEnabled(true);
        });

        // differentiate between short clicks (to select location) & long press-hold (to pan/drag across map)
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartTime = System.currentTimeMillis();
                    viewPager.setUserInputEnabled(false);
                    break;
                case MotionEvent.ACTION_UP:
                    if (System.currentTimeMillis() - touchStartTime < MAX_CLICK_DURATION) {
                        PointF point = new PointF(event.getX(), event.getY());
                        onMapClick(point);
                    }
                    viewPager.setUserInputEnabled(true);
                default:
                    break;
            }
            return false;
        });
    }

    private void setupZoneSpinners(View view) {
        Spinner spnStartZones = view.findViewById(R.id.spinner_start_zones);
        spnEndZones = view.findViewById(R.id.spinner_end_zones);
        int currentZoneIndex = Util.getZoneIndex(zonesMap, ZonedDateTime.now().getOffset());
        String[] offsets = zonesMap.keySet().toArray(new String[0]);
        String[] names = zonesMap.values().toArray(new String[0]);

        SpinnerAdapterZones adapter = new SpinnerAdapterZones(viewPager.getContext(), offsets, names, currentZoneIndex);
        spnStartZones.setAdapter(adapter);
        spnStartZones.setSelection(currentZoneIndex);
        spnStartZones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleSpnItemChange(parent, position);
                zoneIdStart = ZoneId.of(offsets[position]);
                zdtStart = zdtStart.withZoneSameLocal(zoneIdStart);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spnEndZones.setAdapter(adapter);
        spnEndZones.setSelection(currentZoneIndex);
        spnEndZones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleSpnItemChange(parent, position);
                zoneIdEnd = ZoneId.of(offsets[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void handleSpnItemChange(AdapterView<?> parent, int position) {
        maintainScrollPosition();
        SpinnerAdapterZones adapter = (SpinnerAdapterZones) parent.getAdapter();
        adapter.setSelectedPosition(position);
        layoutEndZdt.setVisibility(View.GONE);
    }

    private void setupStartZdtPicker(View view) {
        ConstraintLayout startZdtLayout = view.findViewById(R.id.layout_start_zdt);
        startZdtLayout.setOnClickListener(v -> showDateTimePicker());
        tvStartDate = view.findViewById(R.id.text_start_date);
        tvStartDate.setText(Util.formatZdtDateDayOfWeek(zdtStart));
        tvStartTime = view.findViewById(R.id.text_start_time);
        tvStartTime.setText(Util.formatTimeAmPm(zdtStart));
    }

    private void showDateTimePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(viewPager.getContext());
        View dialogView = LayoutInflater.from(viewPager.getContext()).inflate(R.layout.dialog_datetime_picker, null);
        setupDatePicker(dialogView);
        setupTimePicker(dialogView);
        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> updateStartZdt())
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corners_bg);
        dialog.show();
    }

    private void updateStartZdt() {
        int hr = Integer.parseInt(String.valueOf(hrInput.getText()));
        int min = Integer.parseInt(String.valueOf(minInput.getText()));
        int hr24 = Util.get24HrFrom12Hr(hr, String.valueOf(amPmBtn.getText()));
        zdtStart = zdtStart.withHour(hr24)
                .withMinute(min)
                .withSecond(0)
                .withNano(0)
                .withZoneSameLocal(zoneIdStart);
        tvStartTime.setText(Util.formatTimeAmPm(zdtStart));
        tvStartDate.setText(Util.formatZdtDateDayOfWeek(zdtStart));
    }

    private void setupDatePicker(View dialogView) {
        dateTitle = dialogView.findViewById(R.id.title_datetime_picker);
        CalendarView calendarView = dialogView.findViewById(R.id.calendar_view);
        Calendar calendar = Calendar.getInstance();
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            zdtStart = zdtStart.withYear(year).withMonth(month+1).withDayOfMonth(dayOfMonth);
            updateDateTimePickerTitle(calendar);
        });
        updateDateTimePickerTitle(calendar);
    }

    private void updateDateTimePickerTitle(Calendar calendar) {
        String dayOfWeekStr = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
        assert dayOfWeekStr != null;
        String dayAbbrv = dayOfWeekStr.substring(0, Math.min(dayOfWeekStr.length(), 3));
        String monthStr = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);
        String selectedDate = dayAbbrv + ", " + monthStr + " " + zdtStart.getDayOfMonth();
        dateTitle.setText(selectedDate);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTimePicker(View dialogView) {
        hrInput = dialogView.findViewById(R.id.edit_text_hr);
        int currentHr = zdtStart.getHour();
        int hour12 = (currentHr == 0 || currentHr == 12) ? 12 : currentHr % 12;
        hrInput.setText(String.valueOf(hour12));
        hrInput.setFilters(new InputFilter[] {
                new InputFilterMinMax("1", "12"),
                new InputFilter.LengthFilter(2)
        });

        minInput = dialogView.findViewById(R.id.edit_text_min);
        minInput.setText(String.valueOf(zdtStart.getMinute()));
        minInput.setFilters(new InputFilter[]{
                new InputFilterMinMax("0", "59"),
                new InputFilter.LengthFilter(2)
        });
        minInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String min = String.valueOf(minInput.getText());
                if (min.length() == 1) {
                    String minInputText = Integer.parseInt(min) <= 6 ? min + "0" : "0" + min;
                    minInput.setText(minInputText);
                }
            }
        });

        amPmBtn = dialogView.findViewById(R.id.btn_am_pm);
        amPmBtn.setText(currentHr < 12 ? "AM" : "PM");

        View dialogLayout = dialogView.findViewById(R.id.layout_datetime_picker);
        dialogLayout.setOnTouchListener((v, event) -> {
            // closes keyboard if touch is outside `hour` or `minute` EditText
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (hrInput.hasFocus()) {
                    Util.hideKeyboard(hrInput);
                    hrInput.clearFocus();
                } else if (minInput.hasFocus()) {
                    Util.hideKeyboard(minInput);
                    minInput.clearFocus();
                }
            }
            return false;
        });
    }

    private void setupEndZdtPicker(View view) {
        layoutEndZdt = view.findViewById(R.id.layout_end_zdt);
        layoutEndZdt.setVisibility(View.GONE);
        tvEndTime = view.findViewById(R.id.text_end_time);
        tvEndDate = view.findViewById(R.id.text_end_date);
    }

    private void convertZdt() {
        maintainScrollPosition();
        ZonedDateTime zdtEnd = zdtStart.withZoneSameInstant(zoneIdEnd);
        tvEndTime.setText(Util.formatTimeAmPm(zdtEnd));
        tvEndDate.setText(Util.formatZdtDateDayOfWeek(zdtEnd));
        layoutEndZdt.setVisibility(View.VISIBLE);
    }


    /* get lat/long of clicked position */
    private void onMapClick(PointF point) {
        maintainScrollPosition();
        layoutMap.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        LatLng clickedLatLng = libreMap.getProjection().fromScreenLocation(point);
        getGeoNameData(clickedLatLng);
    }


    /* request clicked location's name data from `GeoNames` */
    private void getGeoNameData(LatLng latlng) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl cityUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("api.geonames.org")
                .addPathSegment("findNearbyPlaceNameJSON")
                .addQueryParameter("lat", String.valueOf(latlng.getLatitude()))
                .addQueryParameter("lng", String.valueOf(latlng.getLongitude()))
                .addQueryParameter("username", BuildConfig.GEONAMES_USERNAME)
                .build();

        Request request = new Request.Builder()
                .url(cityUrl)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    hideMapLocationInfo();
                    Toast.makeText(requireActivity(), "Failed to fetch location data", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String locationName = "";
                assert response.body() != null;
                String responseBody = response.body().string();
                JSONObject jsonObject;
                try {
                    // format fetched location name data
                    jsonObject = new JSONObject(responseBody);
                    JSONArray jsonArray = jsonObject.getJSONArray("geonames");
                    if (jsonArray.length() > 0) {
                        String city = jsonObject.getJSONArray("geonames").getJSONObject(0).getString("adminName1");
                        String country = jsonObject.getJSONArray("geonames").getJSONObject(0).getString("countryName");
                        locationName = city.length() > 0 ? city + ", " + country : country;
                    }
                    getGeoZoneData(latlng, locationName);
                } catch (JSONException e) {
                    requireActivity().runOnUiThread(() -> {
                        hideMapLocationInfo();
                        Toast.makeText(requireActivity(), "Failed to fetch location data", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    /* request clicked location's timezone data from GeoNames */
    private void getGeoZoneData(LatLng latlng, String locName) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl zoneURL = new HttpUrl.Builder()
                .scheme("http")
                .host("api.geonames.org")
                .addPathSegment("timezoneJSON")
                .addQueryParameter("lat", String.valueOf(latlng.getLatitude()))
                .addQueryParameter("lng", String.valueOf(latlng.getLongitude()))
                .addQueryParameter("username", BuildConfig.GEONAMES_USERNAME)
                .build();

        Request request = new Request.Builder()
                .url(zoneURL)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    hideMapLocationInfo();
                    Toast.makeText(getActivity(), "Failed to fetch location data", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String zoneName;
                assert response.body() != null;
                String responseBody = response.body().string();
                JSONObject jsonObject;
                try {
                    // format fetched zone data
                    jsonObject = new JSONObject(responseBody);
                    String timezoneId = jsonObject.getString("timezoneId");
                    zoneIdEnd = ZonedDateTime.now(ZoneId.of(timezoneId)).getOffset();
                    zoneName = timezoneId + " (UTC " + zoneIdEnd + ")";
                    String zoneNameCopy = zoneName;

                    requireActivity().runOnUiThread(() -> {
                        setMapLocationDesc(locName, zoneNameCopy, latlng);
                        int zonePosition = Util.getZoneIndex(zonesMap, (ZoneOffset) zoneIdEnd);
                        if (zonePosition > -1) spnEndZones.setSelection(zonePosition);
                    } );
                } catch (JSONException e) {
                    requireActivity().runOnUiThread(() -> {
                        hideMapLocationInfo();
                        Toast.makeText(requireActivity(), "Failed to fetch location data", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void hideMapLocationInfo() {
        progressBar.setVisibility(View.GONE);
        layoutMap.setVisibility(View.GONE);
        if (marker != null) libreMap.removeMarker(marker);
    }

    private void setMapLocationDesc(String location, String zone, LatLng latLng) {
        progressBar.setVisibility(View.GONE);
        layoutMap.setVisibility(View.VISIBLE);
        tvMapLocationDesc.setText(location);
        tvMapZoneDesc.setText(zone.replaceAll("_", " "));
        addMapMarker(latLng);
    }

    private void addMapMarker(LatLng latlng) {
        if (marker != null) libreMap.removeMarker(marker);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latlng)
                .snippet("(" + latlng.getLatitude() + ", " + latlng.getLongitude() + ")");
        marker = libreMap.addMarker(markerOptions);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}