age com.example.project;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Encode extends AppCompatActivity {

    private static final int SELECT_PICTURE = 100;
    private static final String TAG = "EncodeActivity";

    private TextView statusText;
    private ImageView imageView;
    private EditText messageInput, secretKeyInput;

    private ProgressDialog saveDialog;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode);

        statusText = findViewById(R.id.whether_encoded);
        imageView = findViewById(R.id.imageview);
        messageInput = findViewById(R.id.message);
        secretKeyInput = findViewById(R.id.secret_key);

        Button chooseImageBtn = findViewById(R.id.choose_image_button);
        Button encodeBtn = findViewById(R.id.encode_button);
        Button saveImageBtn = findViewById(R.id.save_image_button);

        checkAndRequestPermissions();

        chooseImageBtn.setOnClickListener(view -> chooseImage());

        encodeBtn.setOnClickListener(view -> {
            if (selectedBitmap == null) {
                statusText.setText("Please select an image.");
                return;
            }

            String message = messageInput.getText().toString();
            String key = secretKeyInput.getText().toString();
            if (message.isEmpty() || key.isEmpty()) {
                statusText.setText("Enter both message and secret key.");
                return;
            }

            Bitmap encodedBitmap = encodeMessage(selectedBitmap, message, key);
            if (encodedBitmap != null) {
                selectedBitmap = encodedBitmap;
                imageView.setImageBitmap(encodedBitmap);
                statusText.setText("Message encoded successfully!");
            } else {
                statusText.setText("Encoding failed. Message too large?");
            }
        });

        saveImageBtn.setOnClickListener(view -> {
            if (selectedBitmap == null) {
                statusText.setText("No image selected.");
                return;
            }

            saveDialog = new ProgressDialog(this);
            saveDialog.setMessage("Saving, Please Wait...");
            saveDialog.setTitle("Saving Image");
            saveDialog.setIndeterminate(false);
            saveDialog.setCancelable(false);
            saveDialog.show();

            new Thread(() -> saveImageToStorage(selectedBitmap)).start();
        });
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imageView.setImageBitmap(selectedBitmap);
                statusText.setText("Image selected.");
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                statusText.setText("Error loading image.");
            }
        }
    }

    private Bitmap encodeMessage(Bitmap original, String message, String key) {
        Bitmap image = original.copy(Bitmap.Config.ARGB_8888, true);
        String encrypted = xorWithKey(message, key);
        encrypted += "\0"; // Null termination


        byte[] messageBytes = encrypted.getBytes();
        StringBuilder binary = new StringBuilder();

        for (byte b : messageBytes) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int totalBits = binary.length();

        if (totalBits > width * height) {
            return null; // Not enough pixels to store message
        }

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= totalBits) break outerLoop;

                int pixel = image.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                int bit = binary.charAt(bitIndex) - '0';
                blue = (blue & 0xFE) | bit;

                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                image.setPixel(x, y, newPixel);

                bitIndex++;
            }
        }

        return image;
    }

    private String xorWithKey(String data, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            result.append((char) (data.charAt(i) ^ key.charAt(i % key.length())));
        }
        return result.toString();
    }


    private void saveImageToStorage(Bitmap bitmap) {
        File file = new File(getExternalFilesDir(null), "Image_" + System.currentTimeMillis() + ".png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            runOnUiThread(() -> {
                saveDialog.dismiss();
                statusText.setText("Saved to:\n" + file.getAbsolutePath());
            });
        } catch (IOException e) {
            Log.e(TAG, "Saving failed", e);
            runOnUiThread(() -> {
                saveDialog.dismiss();
                statusText.setText("Saving failed.");
            });
        }
    }

    private void checkAndRequestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    neededPermissions.toArray(new String[0]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
