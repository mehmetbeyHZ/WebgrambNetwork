package com.webgrambnetwork;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.webgrambnetwork.Models.ApkUpdateResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    EditText phone;
    ProgressBar progressBar;
    SharedPreferences sharedPreferences;
    Button checkVersionBtn;

    /* access modifiers changed from: protected */
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        checkWrites();
        this.sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
        this.phone = (EditText) findViewById(R.id.phoneNumber);
        this.phone.setText(this.sharedPreferences.getString("phoneNumber", null));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.progressBar = findViewById(R.id.progressBar);
        checkVersionBtn = findViewById(R.id.checkVersion);
        (checkVersionBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( !hasPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE ) ||  !hasPermission( Manifest.permission.READ_EXTERNAL_STORAGE ) )
                {
                    ActivityCompat.requestPermissions(
                            SettingsActivity.this,
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                            1
                    );
                }else{
                    checkVersion();
                }
            }
        });
    }


    private void checkWrites()
    {
        if( !hasPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE ) ||  !hasPermission( Manifest.permission.READ_EXTERNAL_STORAGE ) )
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    1
            );
        }
    }

    public boolean hasPermission( String strPerm ) {
        if ( ContextCompat.checkSelfPermission( this, strPerm ) == PackageManager.PERMISSION_GRANTED )
            return true;
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        saveVariables();
        finish();
        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        super.onBackPressed();
        saveVariables();
        finish();
    }

    public void saveVariables() {
        this.sharedPreferences.edit().putString("phoneNumber", this.phone.getText().toString()).apply();
    }

    public void checkVersion()
    {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        final updateService updateService = new updateService();
        Call<ApkUpdateResponse> call = api.getUpdates();
        call.enqueue(new Callback<ApkUpdateResponse>() {
            @Override
            public void onResponse(Call<ApkUpdateResponse> call, Response<ApkUpdateResponse> response) {
                if (response.isSuccessful())
                {
                    if (response.body().getVersionCode() > Double.parseDouble(Config.VERSION_NAME))
                    {
                        progressBar.setVisibility(View.VISIBLE);
                        checkVersionBtn.setEnabled(false);
                        updateService.execute(response.body().getApkUrl(),"WebgrambNetwork.apk");
                    }else{
                        Toast.makeText(SettingsActivity.this,"Uygulama güncel!",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(SettingsActivity.this,"Server Error",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApkUpdateResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }


    public class updateService extends AsyncTask<String, Integer, String> {

        String path;
        int downloaded;
        @Override
        protected String doInBackground(String... strings) {

            String downloadUrl  = strings[0];
            String downloadName = strings[1];
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/"+downloadName;
            try{
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                URL url = new URL(downloadUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(path);

                byte data[] = new byte[2048];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    downloaded = (int) (total * 100 / fileLength);
                    publishProgress(downloaded);
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            return downloadName;
        }

        @Override
        protected void onPostExecute(String downloadName) {
            super.onPostExecute(downloadName);
            progressBar.setVisibility(View.GONE);
            checkVersionBtn.setEnabled(true);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                System.out.println(Environment.getExternalStorageDirectory() + "/Download/" + downloadName);
                File file = new File(Environment.getExternalStorageDirectory() + "/Download/" + downloadName);
                Uri data = FileProvider.getUriForFile(SettingsActivity.this, BuildConfig.APPLICATION_ID + ".provider", file);
                intent.setDataAndType(data, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }else{
                Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
                File file = new File(Environment.getExternalStorageDirectory() + "/Download/" + downloadName);
                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
                String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                myIntent.setDataAndType(Uri.fromFile(file),mimetype);
                startActivity(myIntent);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }
    }


}