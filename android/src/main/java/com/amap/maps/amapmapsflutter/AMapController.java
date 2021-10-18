package com.amap.maps.amapmapsflutter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;

/**
 * @author zxy
 * @data 2018/12/8
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AMapController implements Application.ActivityLifecycleCallbacks, PlatformView, MethodChannel.MethodCallHandler, AMap.OnMapLoadedListener, AMap.OnCameraChangeListener, AMap.OnMarkerClickListener {

    public static final String CHANNEL = "plugins.flutter.maps.amap.com/amap_maps_flutter";
    public static final String METHOD_NAME_CALLBACK_AMAP_ON_MAP_LOADED = "amap#onMapLoaded";
    public static final String METHOD_NAME_CALLBACK_AMAP_ON_CAMERA_CHANGE = "amap#onCameraChange";
    public static final String MEHTOD_NAME_AMAP_CHANGE_CAMERA = "amap#changeCamera";
    public static final String MEHTOD_NAME_AMAP_ADD_MARKER = "amap#addMarker";
    public static final String MEHTOD_NAME_AMAP_UPDATE_MARKER = "amap#updateMarker";
    public static final String MEHTOD_NAME_AMAP_ADD_CLUSTER = "amap#addClusterMarker";

    private Map<Integer, Drawable> mBackDrawAbles = new HashMap<Integer, Drawable>();

    private final Context context;
    private final AtomicInteger activityState;
    private final PluginRegistry.Registrar registrar;
    private final MethodChannel methodChannel;

    private MapView mapView;
    private AMap aMap;

    private boolean disposed = false;

    private final int registrarActivityHashCode;


    private final Map<String, Marker> markers;

    AMapController(int id, Context context,
                   AtomicInteger activityState,
                   PluginRegistry.Registrar registrar) {
        this.context = context;
        this.activityState = activityState;
        this.registrar = registrar;
        this.registrarActivityHashCode = registrar.activity().hashCode();

        registrar.activity().getApplication().registerActivityLifecycleCallbacks(this);

        methodChannel =
                new MethodChannel(registrar.messenger(), CHANNEL + id);
        methodChannel.setMethodCallHandler(this);


        mapView = new MapView(context);
        mapView.onCreate(null);

        aMap = mapView.getMap();

        this.markers = new HashMap<String, Marker>();

        initListener();


    }


    @Override
    public View getView() {
        return mapView;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        mapView.onDestroy();
        registrar.activity().getApplication().unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
//        mapView.onStart();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onResume();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onPause();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
//        mapView.onStop();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onDestroy();
    }

    /**
     * 方法会从flutter中调用
     *
     * @param methodCall
     * @param result
     */
    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

        Marker marker = null;
        MarkerOptions markerOptions = null;

        switch (methodCall.method) {
            case MEHTOD_NAME_AMAP_CHANGE_CAMERA:
//                setText(methodCall, result);

                List<Object> arguments = (List<Object>) methodCall.arguments;
                CameraPosition cameraPosition = Convert.toCameraPosition(arguments.get(0));
                boolean isAnimate = (boolean) arguments.get(1);


                changeCamera(cameraPosition, isAnimate);

                result.success(null);
                break;
            case MEHTOD_NAME_AMAP_ADD_MARKER:

                markerOptions = Convert.toMarkerOptions(methodCall.argument("options"));
                marker = aMap.addMarker(markerOptions);

                markers.put(marker.getId(), marker);

                // 将marker唯一标识传递回去
                result.success(marker.getId());
                break;

            case MEHTOD_NAME_AMAP_UPDATE_MARKER:
                final String markerId = methodCall.argument("marker");

                marker = markers.get(markerId);
                markerOptions = Convert.toMarkerOptions(methodCall.argument("options"));
                marker.setMarkerOptions(markerOptions);

                // 将marker唯一标识传递回去
                result.success(null);
                break;

            case MEHTOD_NAME_AMAP_ADD_CLUSTER:
                markerOptions = Convert.toMarkerOptions(methodCall.argument("options"));
                String name = methodCall.argument("name");
                String count = methodCall.argument("count");
                String color = methodCall.argument("color");
                int radius = dp2px(context, 25);
                BitmapDrawable bitmapDrawable = (BitmapDrawable) getDrawAble(name, count, color, radius);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmapDrawable.getBitmap()));
                marker = aMap.addMarker(markerOptions);
                markers.put(marker.getId(), marker);

                // 将marker唯一标识传递回去
                result.success(marker.getId());
                break;
            default:
                result.notImplemented();
        }
    }


    public void changeCamera(CameraPosition cameraPosition, boolean isAnimate) {
        if (cameraPosition != null) {
            if (isAnimate) {
                aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } else {
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    public Drawable getDrawAble(String name, String count, String color, int radius) {
        Drawable bitmapDrawable = mBackDrawAbles.get(1);
        if (bitmapDrawable == null) {
            bitmapDrawable = new BitmapDrawable(null, drawCircle(name, count, radius,
                    Color.parseColor("#" + color)));
            mBackDrawAbles.put(1, bitmapDrawable);
        }

        return bitmapDrawable;
    }

    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private Bitmap drawCircle(String name, String count, int radius, int color) {
        if (name.length() > 4) {
            name = name.substring(0, 4) + "...";
        }
        if (name.length() == 0) {
            name = " ";
        }
        if (count.length() == 0) {
            count = " ";
        }
        int borderWidth = 10;
        int textSize = radius / 2;
        int middleLineTop = radius/5;

        Bitmap bitmap = Bitmap.createBitmap((radius + borderWidth) * 2, (radius + borderWidth) * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint innerPaint = new Paint();
        Paint outterPaint = new Paint();
        Paint textPaint = new Paint();
        RectF outerArc = new RectF(0, 0, (radius + borderWidth) * 2, (radius + borderWidth) * 2);
        RectF innerArc = new RectF(borderWidth, borderWidth, radius * 2 + borderWidth, radius * 2 + borderWidth);
        outterPaint.setColor(Color.WHITE);
        canvas.drawArc(outerArc, 0, 360, true, outterPaint);
        innerPaint.setColor(color);
        canvas.drawArc(innerArc, 0, 360, true, innerPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int nameSize = textSize * 2 / (name.length() > 1 ? name.length() - 1 : name.length());
        textPaint.setTextSize(nameSize);
        canvas.drawText(name, outerArc.centerX(), outerArc.centerY() - nameSize / 2 + middleLineTop, textPaint);
        int countSize = textSize * 3 / count.length();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(countSize);
        canvas.drawText(count, outerArc.centerX(), outerArc.centerY() + countSize / 2 + 5 + middleLineTop, textPaint);
        return bitmap;
    }

    /**
     * 注册回调监听
     */
    private void initListener() {
        aMap.setOnMapLoadedListener(this);
        aMap.setOnCameraChangeListener(this);

        //覆盖物
        aMap.setOnMarkerClickListener(this);
    }


    @Override
    public void onMapLoaded() {
        if (methodChannel != null) {
            final Map<String, Object> arguments = new HashMap<>(2);
            methodChannel.invokeMethod(METHOD_NAME_CALLBACK_AMAP_ON_MAP_LOADED, arguments);
        }
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        if (methodChannel != null) {
            final Map<String, Object> arguments = new HashMap<>(2);
            arguments.put("position", Convert.toJson(position));
            arguments.put("isFinish", false);
            methodChannel.invokeMethod(METHOD_NAME_CALLBACK_AMAP_ON_CAMERA_CHANGE, arguments);
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition position) {
        final Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("position", Convert.toJson(position));
        arguments.put("isFinish", true);
        methodChannel.invokeMethod(METHOD_NAME_CALLBACK_AMAP_ON_CAMERA_CHANGE, arguments);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
