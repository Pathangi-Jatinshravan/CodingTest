package com.codingtest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter<String> mTrasactions = null;
    private String mJsonString = null;
    Activity mActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTrasactions = new ArrayAdapter<String>(mActivity, R.layout.transaction_view, R.id.list_item_transaction);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FetchTransactions fetchTransactions = new FetchTransactions();
        fetchTransactions.execute();

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mTrasactions);

        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.action_category), R.string.all).apply();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.show_by_category) {
            final ShowAppropriateTransactions transactions = new ShowAppropriateTransactions();
            CharSequence[] items = new CharSequence[4];
            items[0] = getString(R.string.verified);
            items[1] = getString(R.string.unverified);
            items[2] = getString(R.string.fraud);
            items[3] = getString(R.string.all);
            SharedPreferences sharedPreferences = mActivity.getPreferences(Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

            builder.setTitle(getString(R.string.action_category)).setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        editor.putInt(getString(R.string.action_category), R.string.verified).apply();
                        transactions.execute();
                    } else if (which == 1) {
                        editor.putInt(getString(R.string.action_category), R.string.unverified).apply();
                        transactions.execute();
                    } else if (which == 2) {
                        editor.putInt(getString(R.string.action_category), R.string.fraud).apply();
                        transactions.execute();
                    } else if (which == 3) {
                        editor.putInt(getString(R.string.action_category), R.string.all).apply();
                        transactions.execute();
                    }
                }
            }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchTransactions extends AsyncTask<String, Void, String[]> {
        public final String LOG_TAG = FetchTransactions.class.getName();
        String[] results = null;

        @Override
        public String[] doInBackground(String... Params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("https://jsonblob.com/api/jsonBlob/567401b0e4b01190df44ff78");
                urlConnection = (HttpURLConnection) (url.openConnection());
                urlConnection.setRequestMethod("GET");
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();

                if (inputStream != null) {
                    reader = new BufferedReader (new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                }

                mJsonString = builder.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing reading stream " + e);
                    }
                }
            }

            try {
                JSONObject object = new JSONObject(mJsonString);
                JSONArray array = object.getJSONArray("expenses");
                results = new String[array.length()];

                for (int i=0; i<array.length(); i++) {
                    JSONObject transaction = array.getJSONObject(i);
                    String transactionDetails = transaction.getString("description") + '\n' + transaction.getString("time");
                    results[i] = transactionDetails;
                }

                return results;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error " + e);
            }
            return null;
        }

        @Override
        public void onPostExecute(String[] results) {
            if (results != null) {
                mTrasactions.notifyDataSetChanged();
                mTrasactions.clear();
                for (String s:results) {
                    mTrasactions.add(s);
                }
            }
        }
    }

    public class ShowAppropriateTransactions extends AsyncTask<String, Void, String[]> {

        private String LOG_TAG = ShowAppropriateTransactions.class.getName();
        @Override
        public String[] doInBackground(String... params) {
            if (mJsonString == null) {
                return null;
            }
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);
                JSONArray array = jsonObject.getJSONArray("expenses");
                String[] results = new String[array.length()];

                for (int i=0; i<array.length(); i++) {
                    JSONObject transaction = array.getJSONObject(i);

                    SharedPreferences preferences = mActivity.getPreferences(Context.MODE_PRIVATE);
                    int state = preferences.getInt(getString(R.string.action_category), R.string.all);
                    if (!(state == R.string.all)) {
                        if (!transaction.getString("state").equals(getString(state))) {
                            continue;
                        }
                    }

                    String transactionDetails = transaction.getString("description") + '\n' + transaction.getString("time");
                    results[i] = transactionDetails;
                }

                return results;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error " + e);
                return null;
            }
        }

        @Override
        public void onPostExecute(String[] results) {
            if (results != null) {
                mTrasactions.notifyDataSetChanged();
                mTrasactions.clear();
                for (String s:results) {
                    if (s != null) {
                        mTrasactions.add(s);
                    }
                }
            }
        }
    }
}
