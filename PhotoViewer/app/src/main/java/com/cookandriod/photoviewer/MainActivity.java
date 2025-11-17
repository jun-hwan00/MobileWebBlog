package com.cookandriod.photoviewer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_GALLERY = 100;
    private static final int REQUEST_PERMISSION = 101;

    TextView textView;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeRefreshLayout;

    String site_url = "http://10.0.2.2:8000";  // 로컬 테스트용
    // String site_url = "https://junhwan00.pythonanywhere.com";
    String token = "686ed190dde805a5e80a2ee0de839e60466f3274";

    CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // 스와이프 새로고침 설정
        swipeRefreshLayout.setOnRefreshListener(() -> {
            onClickDownload(null);
            swipeRefreshLayout.setRefreshing(false);
        });

        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION);
            }
        } else {
            // Android 12 이하
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "동기화 중...", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            new UploadImageTask().execute(imageUri);
            Toast.makeText(this, "이미지 업로드 중...", Toast.LENGTH_SHORT).show();
        }
    }

    // 업로드 AsyncTask
    private class UploadImageTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            try {
                ContentResolver contentResolver = getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(imageUri);

                if (inputStream == null) {
                    return "ERROR: Cannot open image";
                }

                String fileName = getFileName(imageUri);
                if (fileName == null) {
                    fileName = "upload_" + System.currentTimeMillis() + ".jpg";
                }

                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageBytes = byteBuffer.toByteArray();
                inputStream.close();

                android.util.Log.d("Upload", "Image size: " + imageBytes.length + " bytes");
                android.util.Log.d("Upload", "Filename: " + fileName);

                URL url = new URL(site_url + "/api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("Authorization", "Token " + token);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // author 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("1" + lineEnd);

                // title 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("App Upload" + lineEnd);

                // text 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("Uploaded from Android App" + lineEnd);

                // image 파일
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\""
                        + fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: image/jpeg" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.write(imageBytes);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();

                BufferedReader br;
                if (responseCode >= 200 && responseCode < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                android.util.Log.d("Upload", "Response Code: " + responseCode);
                android.util.Log.d("Upload", "Response: " + response.toString());

                if (responseCode == HttpURLConnection.HTTP_CREATED ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    return "SUCCESS";
                } else {
                    return "FAIL: " + responseCode;
                }

            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("Upload", "Error: " + e.getMessage());
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("SUCCESS")) {
                Toast.makeText(MainActivity.this, "업로드 성공!", Toast.LENGTH_LONG).show();
                textView.setText("업로드 완료! 동기화를 눌러주세요.");
            } else {
                Toast.makeText(MainActivity.this, "업로드 실패: " + result, Toast.LENGTH_LONG).show();
                textView.setText("업로드 실패");
            }
        }
    }

    // 파일 이름 가져오기
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // 다운로드 AsyncTask
    private class CloadImage extends AsyncTask<String, Integer, List<PostData>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }

        @Override
        protected List<PostData> doInBackground(String... urls) {
            List<PostData> postList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return postList;
                }

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                is.close();

                String strJson = result.toString();
                JSONArray aryJson = new JSONArray(strJson);

                for (int i = 0; i < aryJson.length(); i++) {
                    JSONObject post_json = (JSONObject) aryJson.get(i);
                    String imageUrl = post_json.getString("image");
                    String title = post_json.getString("title");
                    String text = post_json.getString("text");
                    String date = post_json.getString("published_date");

                    publishProgress((i + 1) * 100 / aryJson.length());

                    if (!imageUrl.equals("")) {
                        URL myImageUrl = new URL(imageUrl);
                        conn = (HttpURLConnection) myImageUrl.openConnection();
                        InputStream imgStream = conn.getInputStream();
                        Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                        postList.add(new PostData(imageBitmap, title, text, date));
                        imgStream.close();
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                android.util.Log.e("Download", "Error: " + e.getMessage());
            }
            return postList;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<PostData> posts) {
            progressBar.setVisibility(View.GONE);

            if (posts.isEmpty()) {
                textView.setText("⚠️ 불러올 이미지가 없습니다.\n서버 연결을 확인해주세요.");
                Toast.makeText(MainActivity.this, "이미지를 불러올 수 없습니다", Toast.LENGTH_LONG).show();
            } else {
                textView.setText("📸 이미지 로드 성공! (총 " + posts.size() + "개)");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(posts);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
                Toast.makeText(MainActivity.this, "이미지를 클릭하면 상세정보를 볼 수 있습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }
}