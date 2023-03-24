package kz.talipovsn.weather2;

import org.json.JSONArray;
import org.json.JSONObject;
import static kz.talipovsn.weather2.HttpClient.getHTMLData;

public class WeatherBuilder {

    // Получение JSON html-данных погоды по городу и языку
    private static String getWeatherData(String latitude, String longitude) {
        String BASE_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";
        String url = BASE_URL + "?latitude=" + latitude + "&longitude=" + longitude + "&hourly=pm10";
        System.out.println(url);
        return getHTMLData(url);
    }

    // Парсинг даты в формате JSON с созданием объекта погоды
    private static Weather dataParsing(String json) {
        Weather weather = new Weather();
        try {
            JSONObject _obj = new JSONObject(json);
            JSONObject _hourly = _obj.getJSONObject("hourly");
            // JSONArray _time = _hourly.getJSONArray("time");
            JSONArray _time = _hourly.getJSONArray("time");
            JSONArray _pm10 = _hourly.getJSONArray("pm10");
            //  JSONObject _time0 = _time.getJSONObject(0);
            weather.setLatitude(_obj.getString("latitude"));
            weather.setLongitude(_obj.getString("longitude"));
            weather.setTime(_time.getString(25));
            weather.setpm10(Double.parseDouble(_pm10.getString(0)));

        } catch (Exception ignore) {
        }
        return weather;
    }

    // Получение готового объекта погоды по городу и языку
    public static Weather buildWeather (String latitude, String longitude) {
        Weather weather = dataParsing(getWeatherData(latitude, longitude));
        return weather;
    }
}