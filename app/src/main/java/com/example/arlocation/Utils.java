package com.example.arlocation;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Static utility methods to simplify creating multiple demo activities. */
public class Utils {
  private static final String TAG = "Utils";
  private static final double MIN_OPENGL_VERSION = 3.0;

  private Utils() {}
  /**
   * Creates and shows a Toast containing an error message. If there was an exception passed in it
   * will be appended to the toast. The error will also be written to the Log
   */
  public static void displayError(
          final Context context, final String errorMsg, @Nullable final Throwable problem) {
    final String tag = context.getClass().getSimpleName();
    final String toastText;
    if (problem != null && problem.getMessage() != null) {
      Log.e(tag, errorMsg, problem);
      toastText = errorMsg + ": " + problem.getMessage();
    } else if (problem != null) {
      Log.e(tag, errorMsg, problem);
      toastText = errorMsg;
    } else {
      Log.e(tag, errorMsg);
      toastText = errorMsg;
    }

    new Handler(Looper.getMainLooper())
            .post(
                    () -> {
                      Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
                      toast.setGravity(Gravity.CENTER, 0, 0);
                      toast.show();
                    });
  }
  /**
   * Creates an ARCore session. This checks for the CAMERA permission, and if granted, checks the
   * state of the ARCore installation. If there is a problem an exception is thrown. Care must be
   * taken to update the installRequested flag as needed to avoid an infinite checking loop. It
   * should be set to true if null is returned from this method, and called again when the
   * application is resumed.
   *
   * @param activity - the activity currently active.
   * @param installRequested - the indicator for ARCore that when checking the state of ARCore, if
   *     an installation was already requested. This is true if this method previously returned
   *     null. and the camera permission has been granted.
   */
  public static Session createArSession(Activity activity, boolean installRequested)
      throws UnavailableException {
    Session session = null;
    // if we have the camera permission, create the session
      switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
        case INSTALL_REQUESTED:
          return null;
        case INSTALLED:
          break;
      }
      session = new Session(activity);
      // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.
      Config config = new Config(session);
      config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
      session.configure(config);
    return session;
  }

  public static void handleSessionException(
          Activity activity, UnavailableException sessionException) {
    String message;
    if (sessionException instanceof UnavailableArcoreNotInstalledException) {
      message = "Please install ARCore";
    } else if (sessionException instanceof UnavailableApkTooOldException) {
      message = "Please update ARCore";
    } else if (sessionException instanceof UnavailableSdkTooOldException) {
      message = "Please update this app";
    } else if (sessionException instanceof UnavailableDeviceNotCompatibleException) {
      message = "This device does not support AR";
    } else {
      message = "Failed to create AR session";
      Log.e(TAG, "Exception: " + sessionException);
    }
    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "this APP requires Android N or later");
      Toast.makeText(activity, "this APP Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }

  public static String[] needPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
  };

  public static final int PERMISSION_REQUESTCODE = 0;

  public static void checkPermissions(final Activity activity) {
            List<String> needRequestPermissionList = findDeniedPermissions(activity,needPermissions);
            if (needRequestPermissionList != null
                    && needRequestPermissionList.size() > 0) {
                String[] array = needRequestPermissionList.toArray(new String[needRequestPermissionList.size()]);
                activity.requestPermissions(array, PERMISSION_REQUESTCODE);
            }
    }

    public static List<String> findDeniedPermissions(final Activity activity,String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<String>();
        try {
            for (String perm : permissions) {
                Method checkSelfMethod = activity.getClass().getMethod("checkSelfPermission", String.class);
                Method shouldShowRequestPermissionRationaleMethod = activity.getClass().getMethod("shouldShowRequestPermissionRationale",
                        String.class);
                if ((Integer)checkSelfMethod.invoke(activity, perm) != PackageManager.PERMISSION_GRANTED
                        || (Boolean)shouldShowRequestPermissionRationaleMethod.invoke(activity, perm)) {
                    needRequestPermissionList.add(perm);
                }
            }
        } catch (Throwable e) {

        }
        return needRequestPermissionList;
    }

  public static void launchPermissionSettings(final Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
  }

    public static boolean verifyPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void showMissingPermissionDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("提示");
        builder.setMessage("当前应用缺少必要权限。\\n\\n请点击\\\"设置\\\"-\\\"权限\\\"-打开所需权限。");

        // 拒绝, 退出应用
        builder.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });

        builder.setPositiveButton("设置",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchPermissionSettings(activity);
                    }
                });
        builder.setCancelable(false);
        builder.show();
    }

}
