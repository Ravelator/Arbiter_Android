package com.example.ravel.arbiter;

import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Config;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import java.io.File;
import java.io.IOException;

public class UploadFile extends AsyncTask {

    private String filePath = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
            File.separator + Config.IMAGE_DIRECTORY_NAME;
    File dirFile = mkDir(filePath);
    File outputFile = new File(dirFile, String.format("%d.png", System.currentTimeMillis()));
    outStream = new FileOutputStream(outputFile);

                    outStream.write(data);
                    outStream.close();
    opFilePath = outputFile.getAbsolutePath();
    UploadFileToServer uploadFileToServer = new upload();
                    uploadFileToServer.execute();


    @Override
    protected Object doInBackground(Object[] objects) {
        return upload();
    }

    private Object upload() {
        String responseString = null;
        Log.d("Log", "File path" + opFilePath);
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.FILE_UPLOAD_URL);
        try {
            AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                    new AndroidMultiPartEntity.ProgressListener() {

                        @Override
                        public void transferred(long num) {
                            publishProgress((int) ((num / (float) totalSize) * 100));
                        }
                    });
            ExifInterface newIntef = new ExifInterface(opFilePath);
            newIntef.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(2));
            File file = new File(opFilePath);
            entity.addPart("pic", new FileBody(file));
            totalSize = entity.getContentLength();
            httppost.setEntity(entity);

            // Making server call
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity r_entity = response.getEntity();


            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                // Server response
                responseString = EntityUtils.toString(r_entity);
                Log.d("Log", responseString);
            } else {
                responseString = "Error occurred! Http Status Code: "
                        + statusCode + " -> " + response.getStatusLine().getReasonPhrase();
                Log.d("Log", responseString);
            }

        } catch (ClientProtocolException e) {
            responseString = e.toString();
        } catch (IOException e) {
            responseString = e.toString();
        }

        return responseString;
    }

}
