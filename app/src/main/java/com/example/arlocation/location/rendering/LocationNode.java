package com.example.arlocation.location.rendering;

import com.example.arlocation.location.LocationMarker;
import com.example.arlocation.location.LocationScene;
import com.example.arlocation.location.utils.LocationUtils;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class LocationNode extends AnchorNode {

    private String TAG = "LocationNode";

    private LocationMarker locationMarker;
    private LocationNodeRender renderEvent;
    private int distanceInGPS;
    private double distanceInAR;
    private float scaleModifier = 1F;
    private float height = 0F;
    private float gradualScalingMinScale = 0.8F;
    private float gradualScalingMaxScale = 1.4F;

    private LocationMarker.ScalingMode scalingMode = LocationMarker.ScalingMode.FIXED_SIZE_ON_SCREEN;
    private LocationScene locationScene;

    public LocationNode(Anchor anchor, LocationMarker locationMarker, LocationScene locationScene) {
        super(anchor);
        this.locationMarker = locationMarker;
        this.locationScene = locationScene;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getScaleModifier() {
        return scaleModifier;
    }

    public void setScaleModifier(float scaleModifier) {
        this.scaleModifier = scaleModifier;
    }

    public LocationNodeRender getRenderEvent() {
        return renderEvent;
    }

    public void setRenderEvent(LocationNodeRender renderEvent) {
        this.renderEvent = renderEvent;
    }

    public int getDistanceInGPS() {
        return distanceInGPS;
    }

    public double getDistanceInAR() {
        return distanceInAR;
    }

    public void setDistanceInGPS(int distanceInGPS) {
        this.distanceInGPS = distanceInGPS;
    }

    public void setDistanceInAR(double distanceInAR) {
        this.distanceInAR = distanceInAR;
    }

    public LocationMarker.ScalingMode getScalingMode() {
        return scalingMode;
    }

    public void setScalingMode(LocationMarker.ScalingMode scalingMode) {
        this.scalingMode = scalingMode;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        for (Node n : getChildren()) {
            if (getScene() == null) {
                return;
            }

            Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
            Vector3 nodePosition = n.getWorldPosition();

            // Compute the difference vector between the camera and anchor 计算相机与Anchor之间各坐标轴距离
            float dx = cameraPosition.x - nodePosition.x;
            float dy = cameraPosition.y - nodePosition.y;
            float dz = cameraPosition.z - nodePosition.z;

            // Compute the straight-line distanceInAR 计算在AR中的直线距离
            setDistanceInAR(Math.sqrt(dx * dx + dy * dy + dz * dz));

            if (locationScene.shouldOffsetOverlapping()) {
                if (locationScene.mArSceneView.getScene().overlapTestAll(n).size() > 0) {
                    setHeight(getHeight() + 1.2F);
                }
            }
        }

        if(!locationScene.minimalRefreshing())
            scaleAndRotate();

        if (renderEvent != null) {
            if(this.isTracking() && this.isActive() && this.isEnabled())
                renderEvent.render(this);
        }

    }

    public void scaleAndRotate() {

        for (Node n : getChildren()) {
            int markerDistance = (int) Math.ceil(
                    LocationUtils.distance(
                            locationMarker.latitude,
                            locationScene.locationManager.currentLocation.getLatitude(),
                            locationMarker.longitude,
                            locationScene.locationManager.currentLocation.getLongitude())
            );

            setDistanceInGPS(markerDistance);

            // Limit the distanceInGPS of the Anchor within the scene. 限制显示的距离
            int renderDistance = markerDistance;
            if (renderDistance > locationScene.getDistanceLimit())
                renderDistance = locationScene.getDistanceLimit();

            float scale = 1F;

            switch (scalingMode) {
                // Make sure marker stays the same size on screen, no matter the distanceInGPS 所有模型大小一致
                case FIXED_SIZE_ON_SCREEN:
                    scale = 0.5F * (float) renderDistance;
                    // Distant markers a little smaller
                    if (markerDistance > 3000)
                        scale *= 0.75F;
                    break;
                // 模型逐渐变大
                case GRADUAL_TO_MAX_RENDER_DISTANCE:
                    float scaleDifference = gradualScalingMaxScale - gradualScalingMinScale;
                    scale = (gradualScalingMinScale + ((locationScene.getDistanceLimit() - markerDistance) * (scaleDifference / locationScene.getDistanceLimit()))) * renderDistance;
                    break;
                case NO_SCALING:
                    break;
            }

            scale *= scaleModifier;

            Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
            Vector3 nodePosition = n.getWorldPosition();
            // 设置位置
            n.setWorldPosition(new Vector3(n.getWorldPosition().x, getHeight(), n.getWorldPosition().z));
            Vector3 direction = Vector3.subtract(cameraPosition, nodePosition);
            Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
            // 设置朝向
            n.setWorldRotation(lookRotation);
            // 设置大小
            n.setWorldScale(new Vector3(scale, scale, scale));

        }
    }

    public float getGradualScalingMinScale() {
        return gradualScalingMinScale;
    }

    public void setGradualScalingMinScale(float gradualScalingMinScale) {
        this.gradualScalingMinScale = gradualScalingMinScale;
    }

    public float getGradualScalingMaxScale() {
        return gradualScalingMaxScale;
    }

    public void setGradualScalingMaxScale(float gradualScalingMaxScale) {
        this.gradualScalingMaxScale = gradualScalingMaxScale;
    }
}
