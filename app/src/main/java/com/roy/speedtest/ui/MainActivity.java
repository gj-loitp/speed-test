package com.roy.speedtest.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.roy.speedtest.ext.C;
import com.roy.speedtest.helper.Presets;
import com.roy.speedtest.sv.SpeedTestHandler;
import com.roy.speedtest.test.HttpDownloadTest;
import com.roy.speedtest.test.HttpUploadTest;
import com.roy.speedtest.test.PingTest;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import egcodes.com.speedtest.BuildConfig;
import egcodes.com.speedtest.R;
import nl.dionsegijn.konfetti.xml.KonfettiView;


public class MainActivity extends AppCompatActivity {
    static int position = 0;
    static int lastPosition = 0;
    SpeedTestHandler speedTestHandler = null;
    HashSet<String> tempBlackList;

    @Override
    public void onResume() {
        super.onResume();

        speedTestHandler = new SpeedTestHandler();
        speedTestHandler.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        setupViews();
    }

    @SuppressLint("SetTextI18n")
    private void setupViews() {
        final TextView tvVersion = findViewById(R.id.tvVersion);
        final Button btStart = findViewById(R.id.btStart);
        final KonfettiView konfettiView = findViewById(R.id.konfettiView);

        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);
        findViewById(R.id.ivBack).setOnClickListener(view -> {
            onBackPressed();
        });
        findViewById(R.id.ivMenu).setOnClickListener(view -> {
            C.INSTANCE.showPopup(
                    this,
                    view,
                    R.menu.menu_popup,
                    menuItem -> {
                        if (menuItem.getItemId() == R.id.menuRateApp) {
                            C.INSTANCE.rateApp(this, this.getPackageName());
                        } else if (menuItem.getItemId() == R.id.menuMoreApp) {
                            C.INSTANCE.moreApp(this, "Roy93Group");
                        } else if (menuItem.getItemId() == R.id.menuShareApp) {
                            C.INSTANCE.shareApp(this);
                        } else if (menuItem.getItemId() == R.id.menuPolicy) {
                            C.INSTANCE.openBrowserPolicy(this);
                        }
                        return null;
                    });
        });

        final DecimalFormat dec = new DecimalFormat("#.##");
        btStart.setText(getString(R.string.begin_test));

        tempBlackList = new HashSet<>();

        speedTestHandler = new SpeedTestHandler();
        speedTestHandler.start();

        btStart.setOnClickListener(v -> {
            konfettiView.start(Presets.Companion.festive());

//            konfettiView.start(Presets.Companion.explode());
//            konfettiView.start(Presets.Companion.parade());
//            konfettiView.start(Presets.Companion.rain());

            btStart.setEnabled(false);

            //Restart test icin eger baglanti koparsa
            if (speedTestHandler == null) {
                speedTestHandler = new SpeedTestHandler();
                speedTestHandler.start();
            }

            new Thread(new Runnable() {
                RotateAnimation rotate;
                final ImageView ivBar = findViewById(R.id.ivBar);
                final TextView tvPing = findViewById(R.id.tvPing);
                final TextView tvDownload = findViewById(R.id.tvDownload);
                final TextView tvUpload = findViewById(R.id.tvUpload);

                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    runOnUiThread(() -> btStart.setText(R.string.selec_best_sv));

                    //Get egcodes.speedtest hosts
                    int timeCount = 600; //1min
                    while (!speedTestHandler.isFinished()) {
                        timeCount--;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (timeCount <= 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "No Connection...", Toast.LENGTH_LONG).show();
                                btStart.setEnabled(true);
                                btStart.setTextSize(16);
                                btStart.setText(R.string.restart_test);
                            });
                            speedTestHandler = null;
                            return;
                        }
                    }

                    //Find closest server
                    HashMap<Integer, String> mapKey = speedTestHandler.getMapKey();
                    HashMap<Integer, List<String>> mapValue = speedTestHandler.getMapValue();
                    double selfLat = speedTestHandler.getSelfLat();
                    double selfLon = speedTestHandler.getSelfLon();
                    double tmp = 19349458;
                    double dist = 0.0;
                    int findServerIndex = 0;
                    for (int index : mapKey.keySet()) {
                        if (tempBlackList.contains(Objects.requireNonNull(mapValue.get(index)).get(5))) {
                            continue;
                        }

                        Location source = new Location("Source");
                        source.setLatitude(selfLat);
                        source.setLongitude(selfLon);

                        List<String> ls = mapValue.get(index);
                        Location dest = new Location("Dest");
                        assert ls != null;
                        dest.setLatitude(Double.parseDouble(ls.get(0)));
                        dest.setLongitude(Double.parseDouble(ls.get(1)));

                        double distance = source.distanceTo(dest);
                        if (tmp > distance) {
                            tmp = distance;
                            dist = distance;
                            findServerIndex = index;
                        }
                    }
                    String testAddr = Objects.requireNonNull(mapKey.get(findServerIndex)).replace("http://", "https://");
                    final List<String> info = mapValue.get(findServerIndex);
                    final double distance = dist;

                    if (info == null) {
                        runOnUiThread(() -> {
                            btStart.setTextSize(12);
                            btStart.setText(R.string.err_try_again);
                        });
                        return;
                    }

                    runOnUiThread(() -> {
                        btStart.setTextSize(13);
                        btStart.setText(String.format("Host Location: %s [Distance: %s km]", info.get(2), new DecimalFormat("#.##").format(distance / 1000)));
                    });

                    //Init Ping graphic
                    final LinearLayoutCompat layoutChartPing = findViewById(R.id.layoutChartPing);
                    XYSeriesRenderer pingRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine pingFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    pingFill.setColor(Color.parseColor("#4d5a6a"));
                    pingRenderer.addFillOutsideLine(pingFill);
                    pingRenderer.setDisplayChartValues(false);
                    pingRenderer.setShowLegendItem(false);
                    pingRenderer.setColor(Color.parseColor("#4d5a6a"));
                    pingRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiPingRenderer = new XYMultipleSeriesRenderer();
                    multiPingRenderer.setXLabels(0);
                    multiPingRenderer.setYLabels(0);
                    multiPingRenderer.setZoomEnabled(false);
                    multiPingRenderer.setXAxisColor(Color.parseColor("#647488"));
                    multiPingRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                    multiPingRenderer.setPanEnabled(true, true);
                    multiPingRenderer.setZoomButtonsVisible(false);
                    multiPingRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiPingRenderer.addSeriesRenderer(pingRenderer);

                    //Init Download graphic
                    final LinearLayoutCompat layoutChartDownload = findViewById(R.id.layoutChartDownload);
                    XYSeriesRenderer downloadRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine downloadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    downloadFill.setColor(Color.parseColor("#4d5a6a"));
                    downloadRenderer.addFillOutsideLine(downloadFill);
                    downloadRenderer.setDisplayChartValues(false);
                    downloadRenderer.setColor(Color.parseColor("#4d5a6a"));
                    downloadRenderer.setShowLegendItem(false);
                    downloadRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiDownloadRenderer = new XYMultipleSeriesRenderer();
                    multiDownloadRenderer.setXLabels(0);
                    multiDownloadRenderer.setYLabels(0);
                    multiDownloadRenderer.setZoomEnabled(false);
                    multiDownloadRenderer.setXAxisColor(Color.parseColor("#647488"));
                    multiDownloadRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                    multiDownloadRenderer.setPanEnabled(false, false);
                    multiDownloadRenderer.setZoomButtonsVisible(false);
                    multiDownloadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiDownloadRenderer.addSeriesRenderer(downloadRenderer);

                    //Init Upload graphic
                    final LinearLayoutCompat layoutChartUpload = findViewById(R.id.layoutChartUpload);
                    XYSeriesRenderer uploadRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine uploadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    uploadFill.setColor(Color.parseColor("#4d5a6a"));
                    uploadRenderer.addFillOutsideLine(uploadFill);
                    uploadRenderer.setDisplayChartValues(false);
                    uploadRenderer.setColor(Color.parseColor("#4d5a6a"));
                    uploadRenderer.setShowLegendItem(false);
                    uploadRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiUploadRenderer = new XYMultipleSeriesRenderer();
                    multiUploadRenderer.setXLabels(0);
                    multiUploadRenderer.setYLabels(0);
                    multiUploadRenderer.setZoomEnabled(false);
                    multiUploadRenderer.setXAxisColor(Color.parseColor("#647488"));
                    multiUploadRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                    multiUploadRenderer.setPanEnabled(false, false);
                    multiUploadRenderer.setZoomButtonsVisible(false);
                    multiUploadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiUploadRenderer.addSeriesRenderer(uploadRenderer);

                    //Reset value, graphics
                    runOnUiThread(() -> {
                        tvPing.setText("0 ms");
                        layoutChartPing.removeAllViews();
                        tvDownload.setText("0 Mbps");
                        layoutChartDownload.removeAllViews();
                        tvUpload.setText("0 Mbps");
                        layoutChartUpload.removeAllViews();
                    });
                    final List<Double> pingRateList = new ArrayList<>();
                    final List<Double> downloadRateList = new ArrayList<>();
                    final List<Double> uploadRateList = new ArrayList<>();
                    boolean pingTestStarted = false;
                    boolean pingTestFinished = false;
                    boolean downloadTestStarted = false;
                    boolean downloadTestFinished = false;
                    boolean uploadTestStarted = false;
                    boolean uploadTestFinished = false;

                    //Init Test
                    final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 3);
                    final HttpDownloadTest downloadTest = new HttpDownloadTest(testAddr.replace(testAddr.split("/")[testAddr.split("/").length - 1], ""));
                    final HttpUploadTest uploadTest = new HttpUploadTest(testAddr);


                    //Tests
                    while (true) {
                        if (!pingTestStarted) {
                            pingTest.start();
                            pingTestStarted = true;
                        }
                        if (pingTestFinished && !downloadTestStarted) {
                            downloadTest.start();
                            downloadTestStarted = true;
                        }
                        if (downloadTestFinished && !uploadTestStarted) {
                            uploadTest.start();
                            uploadTestStarted = true;
                        }


                        //Ping Test
                        if (pingTestFinished) {
                            //Failure
                            if (pingTest.getAvgRtt() == 0) {
                                System.out.println("Ping error...");
                            } else {
                                //Success
                                runOnUiThread(() -> tvPing.setText(dec.format(pingTest.getAvgRtt()) + " ms"));
                            }
                        } else {
                            pingRateList.add(pingTest.getInstantRtt());

                            runOnUiThread(() -> tvPing.setText(dec.format(pingTest.getInstantRtt()) + " ms"));

                            //Update chart
                            runOnUiThread(() -> {
                                // Creating an  XYSeries for Income
                                XYSeries pingSeries = new XYSeries("");
                                pingSeries.setTitle("");

                                int count = 0;
                                List<Double> tmpLs = new ArrayList<>(pingRateList);
                                for (Double val : tmpLs) {
                                    pingSeries.add(count++, val);
                                }

                                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                dataset.addSeries(pingSeries);

                                GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiPingRenderer);
                                layoutChartPing.addView(chartView, 0);

                            });
                        }


                        //Download Test
                        if (pingTestFinished) {
                            if (downloadTestFinished) {
                                //Failure
                                if (downloadTest.getFinalDownloadRate() == 0) {
                                    System.out.println("Download error...");
                                } else {
                                    //Success
                                    runOnUiThread(() -> tvDownload.setText(dec.format(downloadTest.getFinalDownloadRate()) + " Mbps"));
                                }
                            } else {
                                //Calc position
                                double downloadRate = downloadTest.getInstantDownloadRate();
                                downloadRateList.add(downloadRate);
                                position = getPositionByRate(downloadRate);

                                runOnUiThread(() -> {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    ivBar.startAnimation(rotate);
                                    tvDownload.setText(dec.format(downloadTest.getInstantDownloadRate()) + " Mbps");

                                });
                                lastPosition = position;

                                //Update chart
                                runOnUiThread(() -> {
                                    // Creating an  XYSeries for Income
                                    XYSeries downloadSeries = new XYSeries("");
                                    downloadSeries.setTitle("");

                                    List<Double> tmpLs = new ArrayList<>(downloadRateList);
                                    int count = 0;
                                    for (Double val : tmpLs) {
                                        downloadSeries.add(count++, val);
                                    }

                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(downloadSeries);

                                    GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiDownloadRenderer);
                                    layoutChartDownload.addView(chartView, 0);
                                });

                            }
                        }


                        //Upload Test
                        if (downloadTestFinished) {
                            if (uploadTestFinished) {
                                //Failure
                                if (uploadTest.getFinalUploadRate() == 0) {
                                    System.out.println("Upload error...");
                                } else {
                                    //Success
                                    runOnUiThread(() -> tvUpload.setText(dec.format(uploadTest.getFinalUploadRate()) + " Mbps"));
                                }
                            } else {
                                //Calc position
                                double uploadRate = uploadTest.getInstantUploadRate();
                                uploadRateList.add(uploadRate);
                                position = getPositionByRate(uploadRate);

                                runOnUiThread(() -> {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    ivBar.startAnimation(rotate);
                                    tvUpload.setText(dec.format(uploadTest.getInstantUploadRate()) + " Mbps");
                                });
                                lastPosition = position;

                                //Update chart
                                runOnUiThread(() -> {
                                    // Creating an  XYSeries for Income
                                    XYSeries uploadSeries = new XYSeries("");
                                    uploadSeries.setTitle("");

                                    int count = 0;
                                    List<Double> tmpLs = new ArrayList<>(uploadRateList);
                                    for (Double val : tmpLs) {
                                        if (count == 0) {
                                            val = 0.0;
                                        }
                                        uploadSeries.add(count++, val);
                                    }

                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(uploadSeries);

                                    GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiUploadRenderer);
                                    layoutChartUpload.addView(chartView, 0);
                                });

                            }
                        }

                        //Test bitti
                        if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) {
                            break;
                        }

                        if (pingTest.isFinished()) {
                            pingTestFinished = true;
                        }
                        if (downloadTest.isFinished()) {
                            downloadTestFinished = true;
                        }
                        if (uploadTest.isFinished()) {
                            uploadTestFinished = true;
                        }

                        if (pingTestStarted && !pingTestFinished) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    //Thread bitiminde button yeniden aktif ediliyor
                    runOnUiThread(() -> {
                        btStart.setEnabled(true);
                        btStart.setTextSize(16);
                        btStart.setText("Restart Test");
                    });


                }
            }).start();
        });
    }

    public int getPositionByRate(double rate) {
        if (rate <= 1) {
            return (int) (rate * 30);

        } else if (rate <= 10) {
            return (int) (rate * 6) + 30;

        } else if (rate <= 30) {
            return (int) ((rate - 10) * 3) + 90;

        } else if (rate <= 50) {
            return (int) ((rate - 30) * 1.5) + 150;

        } else if (rate <= 100) {
            return (int) ((rate - 50) * 1.2) + 180;
        }

        return 0;
    }
}

