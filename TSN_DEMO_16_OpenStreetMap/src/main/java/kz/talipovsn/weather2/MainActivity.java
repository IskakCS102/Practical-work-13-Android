package kz.talipovsn.weather2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.GroundOverlay2;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import static kz.talipovsn.weather2.WeatherWidget.updateAllWidgets;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView imageView; // Иконка погоды
    private TextView textView; // Компонент для данных погоды
    private TextView textView2; // Компонент для данных котировок

    private SensorManager mSensorManager;

    private MapView mapView; // Компонент карты


    private String compassDegree = "";
    private int degree = 0;

    private final int PERSMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch (Exception ignore) {
        }

        // Требуемые разрешения для Android >= 6
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };

        // Запрос разрешений, если их еще нет
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERSMISSION_ALL);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);

        JSONWeatherTask task = new JSONWeatherTask(); // Создание потока определения погоды
        task.execute(getString(R.string.latitude), getString(R.string.longitude));

        JSOUPRatesTask task2 = new JSOUPRatesTask(); // Создание потока определения котировок
        task2.execute(); // Активация потока

        // Подключение карт OSM
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Инициализация компонента карты
        mapView = this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Инициализация контроллера карты
        IMapController mapController = (MapController) this.mapView.getController();
        mapController.setZoom(11.0);
        mapController.setCenter(new GeoPoint(52.2772, 76.9865));
        mapController.stopPanning();

        // Установка вращения карты
        final DisplayMetrics dm = this.getResources().getDisplayMetrics();
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(mapView);
        mRotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(mRotationGestureOverlay);

        // Установка масштабирования карты
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mapView);
        mScaleBarOverlay.setScaleBarOffset(0, (int) (40 * dm.density));
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        mapView.getOverlays().add(mScaleBarOverlay);



        // Разрешаем определять текущую позицию
        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        mLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(mLocationOverlay);

        // Разрешаем компас на карте
        CompassOverlay mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        mCompassOverlay.setPointerMode(true);
        mCompassOverlay.enableCompass();
        mapView.getOverlays().add(mCompassOverlay);

        // Устанавливаем маркер 1 на карту
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(new GeoPoint(52.2561941, 77.0422887));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Алюминиевый завод");
        mapView.getOverlays().add(startMarker);

        // Устанавливаем маркер 2 на карту
        Marker startMarker2 = new Marker(mapView);
        startMarker2.setPosition(new GeoPoint(52.3699894, 76.9072292));
        startMarker2.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker2.setTitle("ПНХЗ");
        mapView.getOverlays().add(startMarker2);

        // Обновление виджетов
        updateAllWidgets(this,  new Intent(this, WeatherWidget.class)) ;
    }

    // Действия при окончании установки разрешений для Android >= 6
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERSMISSION_ALL) {
            for (int  result : grantResults)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            reload();
        }
    }

    // Действия проверки нужных разрешений для Android >= 6
    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // Перезагрузка окна
    private void reload() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    // Кнопка "Обновить"
    public void onClick(View view) {
        new JSONWeatherTask().execute(getString(R.string.Город), getString(R.string.Язык)); // Создание и активация потока определения погоды
        new JSOUPRatesTask().execute(); // Создание и активация потока определения котировок
        Toast.makeText(getApplicationContext(), R.string.обновление, Toast.LENGTH_SHORT).show();
        updateAllWidgets(this,  new Intent(this, WeatherWidget.class)) ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Обработчик меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.newCheck) {
            Intent i = new Intent(android.content.Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://play.google.com/store/apps/details?id=kz.proffix4.wrates"));
            startActivity(i);
            return true;
        }

        if (id == R.id.about) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.email, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }

        if (id == R.id.exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        degree = Math.round(event.values[0]);
//        String degreeStr = Weather.getWindDirectionCode(degree);

//        if (!degreeStr.equals(compassDegree)) {
//            compassDegree = degreeStr;
//            new JSONWeatherTask().execute(getString(R.string.Город), getString(R.string.Язык));
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Класс отдельного асинхронного потока
    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        // Тут реализуем фоновую асинхронную загрузку данных, требующих много времени
        @Override
        protected Weather doInBackground(String... params) {
            return WeatherBuilder.buildWeather(params[0], params[1]);
        }

        // Тут реализуем что нужно сделать после окончания загрузки данных
        @Override
        protected void onPostExecute(final Weather weather) {
                super.onPostExecute(weather);


            // Устанавливаем картинку погоды
//            imageView.post(new Runnable() { //  Используем синхронизацию с UI
//                @Override
//                public void run() {
//                    // Если есть считанная иконка с web
//                    if (weather.getIconData() != null) {
//                        imageView.setImageBitmap(weather.getIconData()); // Установка иконки
//                    } else {
//                        imageView.setImageResource(R.mipmap.ic_launcher); // Установка своей иконки при ошибке
//                    }
//                    imageView.invalidate(); // Принудительная прорисовка картинки на всякий случай
//                }
//            });

            // Выдаем данные о погоде в компонент
            textView.post(new Runnable() { //  с использованием синхронизации UI
                @Override
                public void run() {
                    textView.setText("");
                    if (weather.getLatitude() != null) {
                        textView.append(weather.getLatitude() + ", " + weather.getLongitude() + "\n");
                        textView.append(weather.getTime() + "\n");
                        textView.append(String.valueOf(weather.getpm10()) + " C\n");
                    } else {
                        textView.append("Нет данных!" + "\n");
                        textView.append("Проверьте доступность Интернета");
                    }
                }
            });

        }
        // ------------------------------------------------------------------------------------

    }

    // Класс отдельного асинхронного потока
    private class JSOUPRatesTask extends AsyncTask<String, Void, String> {

        // Тут реализуем фоновую асинхронную загрузку данных, требующих много времени
        @Override
        protected String doInBackground(String... params) {
            return RatesReader.getRatesData(); // Получаем данные котировок
        }
        // ----------------------------------------------------------------------------

        // Тут реализуем что нужно сделать после окончания загрузки данных
        @Override
        protected void onPostExecute(final String rates) {
            super.onPostExecute(rates);

            // Выдаем данные о котировках в компонент
            textView.post(new Runnable() { //  с использованием синхронизации UI
                @Override
                public void run() {
                    if (rates != null) {
                        textView2.setText(rates);
                    } else {
                        textView2.setText("");
                        textView2.append(getString(R.string.Нет_данных_о_котировках) + "\n");
                        textView2.append(getString(R.string.Проверьте_доступность_интернета));
                    }
                }
            });

        }
        // ------------------------------------------------------------------------------------

    }

}
