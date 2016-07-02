package com.example.firstplace.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by FirstPlace on 29/06/2016.
 */

public  class ForecastFragment extends Fragment {

   public String forecastJsonStr = null;
    ArrayAdapter<String> adapter;
    Uri.Builder builder;
        public ForecastFragment(){

        }

    public void onCreate(Bundle savedInstances) {
        super.onCreate(savedInstances);
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment,menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();

                }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_main,container,false);
       /* String[] forecastArray = {
                    "Mon 9/10 - Cloudy - 20/17",
                    "Tue 9/11 - Sunny - 25/8",
                    "Wed 9/12 - Cloudy - 21/17",
                    "Thurs 9/13 - Foggy - 18/11",
                    "Fri 9/14 - Rainy - 19/10",
                    "Sat 9/15 - TRAPPED IN WEATHERSTATION - 23/18",
                    "Sun 9/16 - Sunny - 20/7"
        };*/

       // List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));
        adapter = new ArrayAdapter<String>(rootView.getContext(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,new ArrayList<String>());
        ListView list_view_forecast = (ListView) rootView.findViewById(R.id.list_view_forecast);
        list_view_forecast.setAdapter(adapter);

        list_view_forecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = adapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT,forecast);
                startActivity(intent);

            }
        });

        return rootView;

    }
    public void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(getActivity());

        String keylocation = getString(R.string.pref_location_key);
        String defaultLocation=getString(R.string.pref_location_default);
        String location = settings.getString(keylocation,defaultLocation);

        String keyunit = getString(R.string.pref_units_key);
        String defaultunit = getString(R.string.pref_units_default);

        String unit = settings.getString(keyunit,defaultunit);

        weatherTask.execute(location,unit);
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();

    }






    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
        String LOG_TAG=FetchWeatherTask.class.getSimpleName();


        @Override
        protected void onPostExecute(String[] result) {

            if(result != null) {
                try{
                adapter.clear();}catch(Exception e){}


                for (String dayforecastStr : result) {
                    adapter.add(dayforecastStr);
                }
            }
        }

        protected String[] doInBackground(String... Params) {

                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.


                try {
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are avaiable at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    builder = new Uri.Builder();
                    builder.scheme("http");
                    builder.authority("api.openweathermap.org");
                    builder.appendPath("data");
                    builder.appendPath("2.5");
                    builder.appendPath("forecast");
                    builder.appendQueryParameter("q",Params[0]);
                    builder.appendQueryParameter("appid", "dca3bdf26ddbcbd2c250a040654956b3");
                    builder.appendQueryParameter("units", Params[1]);
                    URL url = new URL(builder.build().toString());
                    //Log.v("a",builder.build().toString() );

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    forecastJsonStr = buffer.toString();
                    // Log.v(LOG_TAG,"Forecast Json Strings" + forecastJsonStr);
                    WeatherDataParser weatherdataparser = new WeatherDataParser();
                    try {

                        return weatherdataparser.getWeatherDataFromJson(forecastJsonStr, 7,Params[1]);
                    } catch (JSONException e) {
                    }

                } catch (IOException e) {
                    Log.e("PlaceholderFragment", "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e("PlaceholderFragment", "Error closing stream", e);
                        }
                    }
                }


            return null;
        }

    }


}

