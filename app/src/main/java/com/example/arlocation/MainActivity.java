/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.arlocation;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arlocation.location.LocationMarker;
import com.example.arlocation.location.LocationScene;
import com.example.arlocation.location.rendering.LocationNode;
import com.example.arlocation.location.rendering.LocationNodeRender;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private boolean installRequested;
    private boolean hasFinishedLoadingRenderable = false;
    private boolean hasFinishedSetRenderable = false;
    
    private Snackbar loadingMessageSnackbar = null;
    private ArSceneView arSceneView;
    // Renderables for this example
    private ViewRenderable CCNURenderable;//华中师范大学的标识牌
    private ViewRenderable WHURenderable;//武汉大学的标识牌
    // Our ARCore-Location scene
    private LocationScene locationScene;
    private TextView locationText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Utils.checkIsSupportedDeviceOrFinish(this)) {
            // Not a supported device.
            return;
        }
        Utils.checkPermissions(this);//检查申请权限

        setContentView(R.layout.activity_main);
        arSceneView = findViewById(R.id.ar_scene_view);
        locationText = findViewById(R.id.tv_location);

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> CCNUViewLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.card)
                        .build();

        CompletableFuture<ViewRenderable> WHUViewLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.card)
                        .build();

        CompletableFuture.allOf(
                CCNUViewLayout,
                WHUViewLayout)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().
                            if (throwable != null) {
                                Utils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }
                            try {
                                CCNURenderable = CCNUViewLayout.get();
                                WHURenderable = WHUViewLayout.get();
                                hasFinishedLoadingRenderable = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                Utils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });

        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            //在模型加载完之后再进行接下来的操作
                            if (!hasFinishedLoadingRenderable) {
                                return;
                            }
                            //新建我们继承的LocationScene对象
                            if (locationScene == null) {
                                locationScene = new LocationScene(this, this, arSceneView);
                                locationScene.setOffsetOverlapping(false);//设置是否在重叠的模型上加上偏移量
                            }
                            //获取ArFrame
                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }
                            //当Frame处于跟踪状态再继续
                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }
                            //如果locationScene不为空且还未放置模型
                            if(locationScene != null && !hasFinishedSetRenderable)
                            {
                                //如果当前设备的位置已经获取到
                                if(this.locationScene.locationManager.currentLocation!=null) {
                                    //创建标识华中师范大学的模型
                                    LocationMarker CCNUMarker = new LocationMarker(
                                            114.3541910072,
                                            30.5180109898,
                                            getViewRenderable(CCNURenderable)
                                    );
                                    //设置自定义的渲染事件来展示位置信息
                                    CCNUMarker.setRenderEvent(new LocationNodeRender() {
                                        @Override
                                        public void render(LocationNode node) {
                                            View eView = CCNURenderable.getView();
                                            TextView distanceTextView = eView.findViewById(R.id.tv_message);
                                            String renderInfo = "Central China Normal University"+"\n"
                                                    +"Longitude:"+CCNUMarker.longitude+"\n"
                                                    +"Latitude:"+CCNUMarker.latitude+"\n"
                                                    +node.getDistanceInGPS() + "M";//显示模型与设备之间的距离
                                            distanceTextView.setText(renderInfo);
                                        }
                                    });
                                    //将模型加入locationScene
                                    locationScene.mLocationMarkers.add(CCNUMarker);

                                    //创建标识武汉大学的模型
                                    LocationMarker WuHanMarker = new LocationMarker(
                                            114.3637329340,
                                            30.5399015552,
                                            getViewRenderable(WHURenderable)
                                    );
                                    WuHanMarker.setRenderEvent(new LocationNodeRender() {
                                        @Override
                                        public void render(LocationNode node) {
                                            View eView = WHURenderable.getView();
                                            TextView distanceTextView = eView.findViewById(R.id.tv_message);

                                            String renderInfo = "Wuhan University"+"\n"
                                                    +"Longitude:"+WuHanMarker.longitude+"\n"
                                                    +"Latitude:"+WuHanMarker.latitude+"\n"
                                                    +node.getDistanceInGPS() + "M";
                                            distanceTextView.setText(renderInfo);
                                        }
                                    });
                                    locationScene.mLocationMarkers.add(WuHanMarker);
                                    //模型已经放置完毕
                                    hasFinishedSetRenderable = true;
                                }
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(frame);
                                if(this.locationScene.locationManager.currentLocation!=null)
                                {
                                    String deviceInfo = "WGS Longitude:"+this.locationScene.locationManager.currentLocation.getLongitude()+"\n"
                                            +"WGS Latitude:"+this.locationScene.locationManager.currentLocation.getLatitude()+"\n"
                                            +"AMap Longitude:"+this.locationScene.locationManager.currentAmapLocation.getLongitude()+"\n"
                                            +"AMap Latitude:"+this.locationScene.locationManager.currentAmapLocation.getLatitude()+"\n"
                                            +"Location Type:"+this.locationScene.locationManager.currentAmapLocation.getLocationType()+"\n"
                                        +"Accuracy:"+this.locationScene.locationManager.currentAmapLocation.getAccuracy()+"\n"
                                        +"Address:"+this.locationScene.locationManager.currentAmapLocation.getAddress();
                                    locationText.setText(deviceInfo);
                                }

                            }

                            if (loadingMessageSnackbar != null) {
                                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                                        hideLoadingMessage();//在tracking的时候检测平面才算结束 但非tracKing状态已经可以显示模型了
                                    }
                                }
                            }


                        });
    }

    /**
     * Example node of a layout
     *
     * @return
     */
    private Node getViewRenderable(ViewRenderable renderable) {
        Node base = new Node();
        base.setRenderable(renderable);
        Context c = this;
        // Add  listeners etc here
        View eView = renderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });
        return base;
    }

    /***
     * Example Node of a 3D model
     *
     * @return
     */
    private Node getModelRenderable(ModelRenderable renderable) {
        Node base = new Node();
        base.setRenderable(renderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try
            {
                Session session = Utils.createArSession(this, installRequested);
                if (session == null)
                {
                    installRequested = true;
                    return;
                }
                else
                {
                    arSceneView.setupSession(session);
                }
            }
            catch (UnavailableException e)
            {
                Utils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            Utils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            //showLoadingMessage();
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] paramArrayOfInt) {
        if (requestCode == Utils.PERMISSION_REQUESTCODE) {
            if (!Utils.verifyPermissions(paramArrayOfInt)) {
                finish();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        MainActivity.this.findViewById(android.R.id.content),
                        "seeking plane!!!",
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }
}
