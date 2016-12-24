package cn.edu.pku.zuofei.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zuofei.weatherforecast.R;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.zuofei.app.MyApplication;
import cn.edu.pku.zuofei.bean.City;

/**
 * Created by zuofei on 2016/10/25.
 */
public class SelectCity extends Activity implements View.OnClickListener{
    private ImageView mBackBtn;
    MyApplication App;
    ArrayList<String> city = new ArrayList<String>();
    ArrayList<String> cityId = new ArrayList<String>();
    List<City> data = new ArrayList<City>();
    String SelectedId;
    private EditText m_EditText;
    private String[] mdata;
    private String[] backeupData;
    private ArrayAdapter<String> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState ){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);
        mBackBtn=(ImageView)findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);
        ListView mListView = (ListView) findViewById(R.id.city_list_view);
        App = (MyApplication)getApplication();
        data = App.getCityList();
        int i = 0;
        while(i<data.size()){
            city.add(data.get(i).getCity().toString());
            cityId.add(data.get(i).getNumber().toString());
            i++;
        }
        mdata = new String[data.size()];
        backeupData = new String[data.size()];
        mdata = city.toArray(mdata);
        System.arraycopy(mdata, 0, backeupData, 0, mdata.length);
        final ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                SelectCity.this,android.R.layout.simple_list_item_1,city);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SelectedId =cityId.get(i);
                Intent j =new Intent();
                j.putExtra("cityCode",SelectedId);
                setResult(RESULT_OK,j);
                finish();
            }
        });


        m_EditText =(EditText) findViewById(R.id.search_edit);
        m_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("EditText", "beforeTextChanged");
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchContent = s.toString();
                ArrayList<String> newDataList = new ArrayList<String>();
                for (int i = 0; i < backeupData.length; i++){
                   Log.d("EditText", backeupData[i].substring(0, searchContent.length()));
                    if (backeupData[i].substring(0, searchContent.length()).equals(searchContent)){
                        newDataList.add(backeupData[i]);
                    }
                }
                int j = 0;
                for(; j < newDataList.size(); j++){
                    mdata[j] = newDataList.get(j);
                }

                for (; j < mdata.length; j++){
                    mdata[j] = "";
                }

                adapter.notifyDataSetChanged();
                Log.d("EditText", "onTextChanged" + s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.title_back:
                Intent i =new Intent();
                i.putExtra("cityCode",SelectedId);
                setResult(RESULT_OK,i);
                finish();
                break;
            default:
                break;
        }
    }
}