package kz.talipovsn.weather2;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;


// Класс погоды
public class Weather {
    private String latitude; // Город
    private String longitude; // Страна
    private String time;
    private double pm10; // Температура


    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    public void setTime(String time) {this.time = time;
        System.out.println(time);}
    public void setpm10(double pm10) {
        this.pm10 = pm10;
    }


    public String getLatitude() {
        return latitude;
    }
    public String getLongitude() {
        return longitude;
    }
    public String getTime(){
        SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(date);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);

    }

    public double getpm10() {
        return pm10;
    }
}