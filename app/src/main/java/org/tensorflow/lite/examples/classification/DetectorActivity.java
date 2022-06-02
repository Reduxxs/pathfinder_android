package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.examples.classification.customview.OverlayView;
import org.tensorflow.lite.examples.classification.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.classification.env.BorderedText;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Detector;
import org.tensorflow.lite.examples.classification.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.classification.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.70f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Detector detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 1000;

  private String oldStr = "";

  private String oldTxt = "";

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  private Button btnDistance;
//



  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);
    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
              TFLiteObjectDetectionAPIModel.create(
                      this,
                      TF_OD_API_MODEL_FILE,
                      TF_OD_API_LABELS_FILE,
                      TF_OD_API_INPUT_SIZE,
                      TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing Detector!");
      Toast toast =
              Toast.makeText(
                      getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
            ImageUtils.getTransformationMatrix(
                    previewWidth, previewHeight,
                    cropSize, cropSize,
                    sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    btnDistance = findViewById(R.id.tvDistance);

    btnDistance.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(DetectorActivity.this, Distance.class));
      }
    });

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
            new DrawCallback() {
              @Override
              public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
                if (isDebug()) {
//              tracker.drawDebug(canvas);
                }
              }
            });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);

  }




  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();
    Log.d("myTagNew", String.valueOf(timestamp));
    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }


    TextToSpeech tts = new TextToSpeech(getApplicationContext(), null);
    runInBackground(
            new Runnable() {
              @Override
              public void run() {
                LOGGER.i("Running detection on image " + currTimestamp);
                final long startTime = SystemClock.uptimeMillis();
                final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Style.STROKE);
                paint.setStrokeWidth(2.0f);

                InputImage image = InputImage.fromBitmap(rgbFrameBitmap, sensorOrientation);

                float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                switch (MODE) {
                  case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
                }

                final List<Detector.Recognition> mappedRecognitions =
                        new ArrayList<Detector.Recognition>();
                for (final Detector.Recognition result : results) {
                  final RectF location = result.getLocation();
                  if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);
                    if (result.getConfidence() >= 0.70f && !oldStr.equals(result.getTitle())){

                      //Toast.makeText(getApplicationContext(), "There is an identified " +result.getTitle()+ " " +score + " meter in front of you", Toast.LENGTH_SHORT).show();
                      tts.speak("There is an identified " + result.getTitle() + "in front of you", TextToSpeech.QUEUE_FLUSH, null);
                      oldStr = result.getTitle();
                    }

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);
                  }
                }

                tracker.trackResults(mappedRecognitions, currTimestamp);
                trackingOverlay.postInvalidate();

                computingDetection = false;

                runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            showFrameInfo(previewWidth + "x" + previewHeight);
//                            showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                            showInference(lastProcessingTimeMs + "ms");
                          }
                        });
              }
            });
  }


  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  public String LoadData(String inFile) {
    String tContents = "";

    try {
      InputStream stream = getAssets().open(inFile);

      int size = stream.available();
      byte[] buffer = new byte[size];
      stream.read(buffer);
      stream.close();
      tContents = new String(buffer);
    } catch (IOException e) {
      // Handle exceptions here
    }

    return tContents;

  }

  public static double similarity(String s1, String s2) {
    String longer = s1, shorter = s2;
    if (s1.length() < s2.length()) { // longer should always have greater length
      longer = s2; shorter = s1;
    }
    int longerLength = longer.length();
    if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
    return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
  }

  public static int editDistance(String s1, String s2) {
    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();

    int[] costs = new int[s2.length() + 1];
    for (int i = 0; i <= s1.length(); i++) {
      int lastValue = i;
      for (int j = 0; j <= s2.length(); j++) {
        if (i == 0)
          costs[j] = j;
        else {
          if (j > 0) {
            int newValue = costs[j - 1];
            if (s1.charAt(i - 1) != s2.charAt(j - 1))
              newValue = Math.min(Math.min(newValue, lastValue),
                      costs[j]) + 1;
            costs[j - 1] = lastValue;
            lastValue = newValue;
          }
        }
      }
      if (i > 0)
        costs[s2.length()] = lastValue;
    }
    return costs[s2.length()];
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  protected void onInferenceConfigurationChanged() {

  }


  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }
}
