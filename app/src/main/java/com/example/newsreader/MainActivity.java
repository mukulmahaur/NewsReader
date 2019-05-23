package com.example.newsreader;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listview);
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titles);

        database = this.openOrCreateDatabase("Articlesnew",MODE_PRIVATE,null);
        database.execSQL("CREATE TABLE IF NOT EXISTS articlesnew (id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR ,url VARCHAR)");

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ContentPage.class);
                intent.putExtra("url",urls.get(position));
                startActivity(intent);
            }
        });
        updateListView();
        DownloadTask task = new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateListView(){
        Cursor c = database.rawQuery("SELECT * FROM articlesnew",null);
        int urlIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            urls.clear();
            do{
                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }


    public class DownloadTask extends AsyncTask<String ,Void,String>{

        public String getDataFromUrl(String temp) throws IOException {
            String result = "";
            try{
                URL url;
                HttpURLConnection urlConnection = null;
                url = new URL(temp);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data!=-1){
                    char current = (char)data;
                    result += current;
                    data = reader.read();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected String doInBackground(String... strings) {

            try {
                String ids = getDataFromUrl(strings[0]);
//                Log.i("Getting Ids",ids);
                JSONArray jsonArray = new JSONArray(ids);
                database.execSQL("DELETE FROM articlesnew");
                for(int i=0;i<20;i++){
                    String article = getDataFromUrl("https://hacker-news.firebaseio.com/v0/item/"+jsonArray.getString(i)+".json?print=pretty");
                    Log.i("Geting details",article);
                    JSONObject jsonObject = new JSONObject(article);
//                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleId = jsonObject.getString("id");
                        String articleUrl = jsonObject.getString("url");
//                        String articleContent = getDataFromUrl(articleUrl);
                        String sql = "INSERT INTO articlesnew (id,articleId,title,url) VALUES ("+i+","+articleId+",\""+articleTitle+"\",\""+articleUrl+"\")";
                        database.execSQL(sql);
//                    String sql = "INSERT INTO articles (articleId,title,url) VALUES (? , ? , ?)";
//                    SQLiteStatement statement = database.compileStatement(sql);
//                    statement.bindString(1,articleId);
//                    statement.bindString(2,articleTitle);
//                    statement.bindString(3,articleUrl);
//                    statement.execute();


//                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
