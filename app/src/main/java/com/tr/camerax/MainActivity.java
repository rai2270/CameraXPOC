package com.tr.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("RestrictedApi")
public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Preview imagePreview;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private CameraControl cameraControl;
    private CameraInfo cameraInfo;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean isRecording = false;
    private boolean videoBindComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        if (allPermissionsGranted()) {
            previewView.post(() -> {
                bindCamera();
            });
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        findViewById(R.id.vidCaptureBind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoCapture == null) {
                    videoBindComplete = false;
                    bindVideo();
                }
            }
        });

        findViewById(R.id.vidCaptureStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoBindComplete && videoCapture != null) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    startRecording();
                }
            }
        });

        findViewById(R.id.vidCaptureStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoBindComplete && videoCapture != null) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    videoCapture.stopRecording();
                }
            }
        });
    }

    private void bindCamera() {
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    imagePreview = new Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .setTargetRotation(previewView.getDisplay().getRotation())
                            .build();
                    imagePreview.setSurfaceProvider(previewView.createSurfaceProvider());

                    imageCapture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                            .build();

                    CameraSelector cameraSelector =
                            new CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build();

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    // Must unbind the use-cases before rebinding them
                    cameraProvider.unbindAll();

                    Camera camera = cameraProvider.bindToLifecycle(
                            (LifecycleOwner) MainActivity.this,
                            cameraSelector,
                            imagePreview,
                            imageCapture
                    );
                    cameraControl = camera.getCameraControl();
                    cameraInfo = camera.getCameraInfo();
//                setTorchStateObserver()
//                setZoomStateObserver()
                } catch (Exception e) {
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Pic captured at " + file.getAbsolutePath();
                previewView.post(() -> {
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                String msg = "Pic capture failed : " + exception.getMessage();
                previewView.post(() -> {
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void bindVideo() {
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    imagePreview = new Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .setTargetRotation(previewView.getDisplay().getRotation())
                            .build();
                    imagePreview.setSurfaceProvider(previewView.createSurfaceProvider());

                    videoCapture = new VideoCapture.Builder()
                            //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build();

                    CameraSelector cameraSelector =
                            new CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build();

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    // Must unbind the use-cases before rebinding them
                    cameraProvider.unbindAll();

                    Camera camera = cameraProvider.bindToLifecycle(
                            (LifecycleOwner) MainActivity.this,
                            cameraSelector,
                            imagePreview,
                            videoCapture
                    );
                    cameraControl = camera.getCameraControl();
                    cameraInfo = camera.getCameraInfo();
                } catch (Exception e) {
                }

                previewView.post(() -> {
                    Toast.makeText(getBaseContext(), "Video Bind Complete", Toast.LENGTH_SHORT).show();
                });

                videoBindComplete = true;
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startRecording() {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".mp4");
        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(file).build();
        if (videoCapture != null) {
            previewView.post(() -> {
                Toast.makeText(getBaseContext(), "Video Start", Toast.LENGTH_SHORT).show();
            });
            videoCapture.startRecording(outputFileOptions, executor, new VideoCapture.OnVideoSavedCallback() {
                @Override
                public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                    String msg = "Video captured at " + file.getAbsolutePath();
                    previewView.post(() -> {
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    });

                    isRecording = false;
                }

                @Override
                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    isRecording = false;
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                bindCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
