package com.tale.camera;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by TALE on 12/31/2014.
 */
public abstract class CameraFragment extends Fragment implements Camera.PictureCallback {
    /**
     * ID value for a particular camera (front or back) that was not found.
     */
    private static final int NO_CAMERA = -1;

    /**
     * Whether the currently open camera is the front-facing camera.
     */
    private static final String STATE_IS_FRONT_CAMERA = "isFrontCamera";

    // Views
    private PreviewSurface mPreview;

    // Camera fields
    private Camera mCamera;

    private boolean mIsFrontCamera;
    private int mBackCameraId;
    private int mFrontCameraId;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPreview = new PreviewSurface(getActivity());
        // Grab references to the SurfaceView for the preview and the TextView for display errors.
        return mPreview;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
            If the device actually has a camera, set up the surface holder.
            Otherwise, display an error message.
        */
        if (hasCamera()) {
            /*
                If the activity contains a valid SurfaceView for the preview, then set it up.
                Otherwise, display an error message.
             */
            if (mPreview != null) {
                // Get back-facing camera info.
                mBackCameraId = findCameraId(false);

                // If the device has a front-facing camera, determine what camera we open first.
                if (hasFrontCamera()) {
                    mFrontCameraId = findCameraId(true);
                    mIsFrontCamera = savedInstanceState != null
                            && savedInstanceState.getBoolean(STATE_IS_FRONT_CAMERA, false);
                }

            } else {
                throw new IllegalStateException("Preview does not exist. Make sure super.onViewCreated() is not dropped");
            }
        } else {
            onNoCamera();
        }
    }

    protected abstract void onNoCamera();

    @Override
    public void onResume() {
        super.onResume();

        // If there is a hardware camera then open it and start setting up the preview surface.
        if (mPreview != null && hasCamera()) {
            openCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Close the camera while we are not using so that other applications can use it.
        closeCamera();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save which camera is currently open to state.
        outState.putBoolean(STATE_IS_FRONT_CAMERA, mIsFrontCamera);
    }


    //
    // Camera setup
    //

    /**
     * Check whether the device actually has a camera.
     *
     * @return True if the device has a camera, false otherwise.
     */
    private boolean hasCamera() {
        final PackageManager packageManager = getPackageManager();
        return packageManager != null
                && packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * Check whether the device has a front-facing camera.
     *
     * @return True if the device has a front-facing camera; false otherwise.
     */
    private boolean hasFrontCamera() {
        final PackageManager packageManager = getPackageManager();
        return packageManager != null
                && packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    private PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    /**
     * Returns the camera ID (an integer between 0 and {@link android.hardware.Camera#getNumberOfCameras()})
     * for either the first front-facing or first back-facing camera.
     *
     * @param front True to find the first front-facing camera; false to find the first back-facing
     *              camera.
     * @return The camera ID for the requested camera as an integer between between 0 and {@link
     * android.hardware.Camera#getNumberOfCameras()} or -1 if there was no matching camera.
     */
    private int findCameraId(boolean front) {
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int cameraCount = Camera.getNumberOfCameras();
        /*  Iterate through 0…getNumberOfCameras - 1 to find the camera ID of the first front or
        the first back camera. */
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if ((front && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    || (!front && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
                return i;
            }
        }
        return NO_CAMERA;
    }

    /**
     * Returns a {@link android.hardware.Camera.CameraInfo} instance containing information on the
     * currently open camera.
     *
     * @return A {@link android.hardware.Camera.CameraInfo} instance containing information on the
     * currently open camera or `null` if no camera is open.
     */
    private Camera.CameraInfo getCameraInfo() {
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mIsFrontCamera ? mFrontCameraId : mBackCameraId, cameraInfo);
        return cameraInfo;
    }

    /**
     * Open the first back-facing camera and grab a {@link android.hardware.Camera} instance.
     */
    private void openCamera() {
        if (mCamera != null) {
            mCamera.release();
        }

        mCamera = Camera.open(mIsFrontCamera ? mFrontCameraId : mBackCameraId);
        final Camera.CameraInfo cameraInfo = getCameraInfo();
        mPreview.setCamera(mCamera, cameraInfo);
        mPreview.start();
    }

    /**
     * Close the camera and release the previously obtained {@link android.hardware.Camera} instance
     * to make sure that other applications can grab the camera if needed.
     */
    private void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mPreview.reset();
        }
    }

    /**
     * Toggles the camera if we have two cameras available and starts the preview for the new
     * camera.
     */
    protected void switchCamera() {
        if (mFrontCameraId != NO_CAMERA) {
            mIsFrontCamera = !mIsFrontCamera;
            openCamera();
        }
    }

    /**
     * Checks if the camera is open and takes a picture, retrieving JPEG data.
     */
    protected void takePicture() {
        if (mCamera != null) {
            // Take picture and capture raw image data.
            mCamera.takePicture(null, null, this);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

    }
}
