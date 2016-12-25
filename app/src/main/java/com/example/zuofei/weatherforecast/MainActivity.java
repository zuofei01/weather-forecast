package com.example.zuofei.weatherforecast;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.pku.zuofei.bean.TodayWeather;
import cn.edu.pku.zuofei.miniweather.SelectCity;
import cn.edu.pku.zuofei.util.NetUtil;

/**
 * Created by zuofei on 2016/9/21.
 */
public class MainActivity extends Activity implements View.OnClickListener,ViewPager.OnPageChangeListener {
    private static final int UPDATE_TODAY_WEATHER = 1,UPDATE_TODAY_FAIL = 2,SIX_DAY_WEATHER = 3;
    private static final int SDK_PERMISSION_REQUEST = 100;
    private ImageView mUpdateBtn;
    private ImageView mCitySelect;
    private ImageView mLocationBtn;
    private LocationManager locationManager;
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv;
    private ImageView weatherImg, pmImg;
    private AnimationDrawable _animationDrawable;

    //ViewPage一周天气
    private ViewPager vp;
    private ViewPageAdapter vpAdapter;
    private List<View> views;

    private static final int[] pics = {R.layout.weatherpage1,R.layout.weatherpage1};
    private ImageView[] dots;
    private int currentIndex;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                case UPDATE_TODAY_FAIL:
                    if(!mUpdateBtn.isClickable()){
                        mUpdateBtn.setClickable( true );
                        _animationDrawable.stop();
                        mUpdateBtn.setImageResource( R.drawable.title_update );
                    }
                    break;
                case SIX_DAY_WEATHER:
                    updateSixDayWeaher((ArrayList<TodayWeather>) msg.obj);
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mLocationBtn = (ImageView) findViewById(R.id.title_location);
        mLocationBtn.setOnClickListener(this);
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络ok");
            Toast.makeText(MainActivity.this, "网络ok", Toast.LENGTH_LONG).show();
        } else {
            Log.d("myWeather", "网络挂了");
            Toast.makeText(MainActivity.this, "网络挂了", Toast.LENGTH_LONG).show();
        }
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);
        initView();
    }
    private List<TodayWeather> parseSixDayXML(String xmlData) {
        List<TodayWeather> sixDays = new ArrayList<TodayWeather>(  );

        boolean typeCount = true;//获取白天的天气类型

        TodayWeather weather = null;
        try{
            XmlPullParserFactory fac =XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(  xmlData ));
            int eventType = xmlPullParser.getEventType();
            Log.d( "myWeather","parser" );
            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals( "yesterday" ) || xmlPullParser.getName().equals( "weather" )){
                            weather = new TodayWeather();
                        }
                        if(weather != null){
                            if(xmlPullParser.getName().equals( "date_1" ) || xmlPullParser.getName().equals( "date" )){
                                xmlPullParser.next();
                                weather.setDate( xmlPullParser.getText()  );
                            }else if(xmlPullParser.getName().equals( "high_1" ) || xmlPullParser.getName().equals( "high" )){
                                xmlPullParser.next();
                                weather.setHigh( xmlPullParser.getText().substring( 2 ).trim());
                            }else if(xmlPullParser.getName().equals( "low_1" ) || xmlPullParser.getName().equals( "low" )){
                                xmlPullParser.next();
                                weather.setLow( xmlPullParser.getText().substring( 2 ).trim() );
                            }else if(xmlPullParser.getName().equals( "type_1" ) || xmlPullParser.getName().equals( "type" )){
                                if(typeCount){

                                    xmlPullParser.next();
                                    weather.setType( xmlPullParser.getText() );
                                    typeCount = false;

                                }else
                                    typeCount = true;
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(xmlPullParser.getName().equals( "yesterday" ) || xmlPullParser.getName().equals( "weather" )){
                            sixDays.add( weather );
                        }
                        break;

                }
                eventType = xmlPullParser.next();
            }
        }catch (XmlPullParserException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return sixDays;
    }

    private void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather", address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                List<TodayWeather> sixDays = null;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(8000);
                    con.setRequestMethod("GET");
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        response.append(str);
                        Log.d("myWeather", str);
                    }
                    String responseStr = response.toString();
                    Log.d("myWeather", responseStr);
                    todayWeather = parseXML(responseStr);
                    sixDays = parseSixDayXML( responseStr );
                    if (todayWeather != null&& sixDays != null) {
                        Log.d("myWeather", todayWeather.toString());
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                        msg = new Message();
                        msg.what = SIX_DAY_WEATHER;
                        msg.obj = sixDays;
                        mHandler.sendMessage( msg );


                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = UPDATE_TODAY_FAIL;
                    msg.obj = todayWeather;
                    mHandler.sendMessage( msg );
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }

            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.title_city_manager) {
            Intent i = new Intent(this, SelectCity.class);
            //startActivity(i);
            startActivityForResult(i, 1);
        }
        if (view.getId() == R.id.title_update_btn) {
            int pivotType = Animation.RELATIVE_TO_SELF; // 相对于自己
            float pivotX = .5f; // 取自身区域在X轴上的中心点
            float pivotY = .5f; // 取自身区域在Y轴上的中心点
            RotateAnimation animation = new RotateAnimation(0f, 360f, pivotType, pivotX, pivotType, pivotY);
            animation.setDuration(1000);
            animation.setRepeatCount(5);
            animation.setRepeatMode(Animation.RESTART);
            mUpdateBtn.startAnimation(animation);
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");
            Log.d("myWeather", cityCode);
            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络ok");
                queryWeatherCode(cityCode);
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
                mUpdateBtn.clearAnimation();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCityCode = data.getStringExtra("cityCode");
            Log.d("myweather", "选择的城市代码" + newCityCode);
            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络ok");
                queryWeatherCode(newCityCode);
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("resp")) {
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    void initView() {
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);

        city_name_Tv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        //ViewPage
        LayoutInflater inflater = LayoutInflater.from( this );
        views = new ArrayList<View>();
        for(int i = 0; i< pics.length; i++){
            views.add( inflater.inflate( pics[i],null ) );
        }
        vpAdapter = new ViewPageAdapter( views,this );
        vp = (ViewPager) findViewById( R.id.six_day_info );
        vp.setAdapter( vpAdapter );
        initDots();
        vp.setOnPageChangeListener( this );

    }
    private void initDots() {
        LinearLayout l1 = (LinearLayout) findViewById( R.id.weather_info_page);
        l1.setOnClickListener( this );

        dots = new ImageView[pics.length];
        for(int i = 0; i < pics.length; i++){
            dots[i] = (ImageView) l1.getChildAt( i );
            dots[i].setEnabled( false );
            dots[i].setTag( i );
        }
        currentIndex = 0;
        dots[currentIndex].setEnabled( true );
    }
    private void updateSixDayWeaher(ArrayList<TodayWeather> wList) {


        TextView date,temperature,climate;
        ImageView type;
        TodayWeather weather = null;
        int[] ids = {R.id.p1,R.id.p2,R.id.p3};
        for(int i = 0; i < wList.size();i++ ){
            date = (TextView) views.get( i / 3 ).findViewById( ids[i % 3] ).findViewById( R.id.date );
            temperature = (TextView) views.get( i / 3 ).findViewById( ids[i % 3] ).findViewById( R.id.day_temp );
            climate = (TextView) views.get( i / 3 ).findViewById( ids[i % 3] ).findViewById( R.id.day_weather );
            type = (ImageView) views.get( i / 3 ).findViewById(ids[i % 3] ).findViewById( R.id.day_weather_img );
            weather = wList.get( i );
            Log.d( "sixDay", weather.toString());
            if(weather.getDate() != null){
                date.setText( weather.getDate() );
            }else{
                date.setText( "not found" );
            }
            if(weather.getHigh() != null && weather.getLow()!= null){
                temperature.setText( weather.getHigh() + "~" + weather.getLow() );
            }else{
                temperature.setText( "not found" );
            }

            if(weather.getType() != null){
                refreshWeatherType( climate,type,weather );
            }else{
                climateTv.setText( "not found" );
            }
        }
        Toast.makeText(  MainActivity.this,"更新成功！", Toast.LENGTH_LONG ).show();
    }
    private void refreshWeatherType(TextView climateTv, ImageView weatherImg,TodayWeather todayWeather){
        Resources res = getResources();
        climateTv.setText( todayWeather.getType() );
        if(todayWeather.getType().equals( "暴雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_baoxue ) );
        }else if(todayWeather.getType().equals( "暴雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_baoyu ) );
        }else if(todayWeather.getType().equals( "大暴雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_dabaoyu ) );
        }else if(todayWeather.getType().equals( "大雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_daxue ) );
        }else if(todayWeather.getType().equals( "大雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_dayu ) );
        }else if(todayWeather.getType().equals( "多云" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_duoyun ) );
        }else if(todayWeather.getType().equals( "雷阵雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_leizhenyu ) );
        }else if(todayWeather.getType().equals( "雷阵雨冰雹" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_leizhenyubingbao ) );
        }else if(todayWeather.getType().equals( "晴" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_qing ) );
        }else if(todayWeather.getType().equals( "沙尘暴" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_shachenbao ) );
        }else if(todayWeather.getType().equals( "特大暴雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_tedabaoyu ) );
        }else if(todayWeather.getType().equals( "雾" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_wu ) );
        }else if(todayWeather.getType().equals( "小雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_xiaoxue ) );
        }else if(todayWeather.getType().equals( "小雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_xiaoyu ) );
        }else if(todayWeather.getType().equals( "阴" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_yin ) );
        }else if(todayWeather.getType().equals( "雨夹雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_yujiaxue ) );
        }else if(todayWeather.getType().equals( "阵雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_zhenxue ) );
        }else if(todayWeather.getType().equals( "阵雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_zhenyu ) );
        }else if(todayWeather.getType().equals( "中雨" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_zhongyu ) );
        }else if(todayWeather.getType().equals( "中雪" )){
            weatherImg.setImageDrawable( res.getDrawable( R.drawable.biz_plugin_weather_zhongxue ) );
        }

    }
    void updateTodayWeather(TodayWeather todayWeather) {
        city_name_Tv.setText(todayWeather.getCity() + "天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + "发布");
        humidityTv.setText("湿度：" + todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        String pm25 = todayWeather.getPm25();
       if (pm25 == null){
           pmDataTv.setText("N/A");
           pmQualityTv.setText("N/A");
       }else{
           int pm = Integer.parseInt(pm25);
           if (pm <= 50) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
           } else if (pm <= 100 && pm > 50) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
           } else if (pm <= 150 && pm > 100) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
           } else if (pm <= 200 && pm > 150) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
           } else if (pm <= 300 && pm > 200) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
           } else if (pm > 300) {
               pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
           }
           pmQualityTv.setText(todayWeather.getQuality());
       }

        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getHigh() + "~" + todayWeather.getLow());
        climateTv.setText(todayWeather.getType());
        if (todayWeather.getType().equals("晴")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
        } else if (todayWeather.getType().equals("暴雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
        } else if (todayWeather.getType().equals("暴雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoyu);
        } else if (todayWeather.getType().equals("大暴雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_dabaoyu);
        } else if (todayWeather.getType().equals("大雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_daxue);
        } else if (todayWeather.getType().equals("大雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_dayu);
        } else if (todayWeather.getType().equals("多云")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_duoyun);
        } else if (todayWeather.getType().equals("雷阵雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyu);
        } else if (todayWeather.getType().equals("雷阵雨冰雹")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyubingbao);
        } else if (todayWeather.getType().equals("沙尘暴")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_shachenbao);
        } else if (todayWeather.getType().equals("特大暴雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_tedabaoyu);
        } else if (todayWeather.getType().equals("雾")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_wu);
        } else if (todayWeather.getType().equals("小雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
        } else if (todayWeather.getType().equals("小雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
        } else if (todayWeather.getType().equals("阴")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yin);
        } else if (todayWeather.getType().equals("雨夹雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yujiaxue);
        } else if (todayWeather.getType().equals("阵雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
        } else if (todayWeather.getType().equals("阵雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenyu);
        } else if (todayWeather.getType().equals("中雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
        } else if (todayWeather.getType().equals("中雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
        }
        windTv.setText("风力:" + todayWeather.getFengli());
        Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_SHORT).show();
        mUpdateBtn.clearAnimation();
    }

    public boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(position < 0 || position >= pics.length  || currentIndex == position){
            return;
        }
        dots[position].setEnabled( true );
        dots[currentIndex] .setEnabled( false );
        currentIndex = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}

