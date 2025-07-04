package com.example.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class Decode extends AppCompatActivity {

    private static final String TAG = "DecodeActivity";

    private TextView statusText;
    private ImageView imageView;
    private TextView messageField;
    private EditText secretKeyField;
    private Bitmap selectedImage;
    private Uri filepath;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                                filepath = result.getData().getData();
                                try {
                                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), filepath);
                                    imageView.setImageBitmap(selectedImage);
                                    statusText.setText("Image loaded successfully.");
                                } catch (IOException e) {
                                    Log.e(TAG, "Image loading failed", e);
                                    statusText.setText("Failed to load image.");
                                }
                            } else {
                                statusText.setText("No image selected.");
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        statusText = findViewById(R.id.status_text);
        imageView = findViewById(R.id.image_view);
        messageField = findViewById(R.id.message_field);
        secretKeyField = findViewById(R.id.secret_key_field);

        Button chooseImageButton = findViewById(R.id.choose_image_button);
        Button decodeButton = findViewById(R.id.decode_button);

        chooseImageButton.setOnClickListener(v -> openImageChooser());

        decodeButton.setOnClickListener(v -> {
            if (selectedImage != null) {
                String key = secretKeyField.getText().toString();
                if (key.isEmpty()) {
                    statusText.setText("Please enter the secret key.");
                    return;
                }

                String decoded = decodeMessage(selectedImage, key);
                if (decoded != null && !decoded.isEmpty()) {
                    messageField.setText(decoded);
                    statusText.setText("Message decoded successfully!");
                } else {
                    statusText.setText("No hidden message found.");
                    messageField.setText("");
                }
            } else {
                statusText.setText("Please select an image first.");
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select an Image"));
    }

    private String decodeMessage(Bitmap image, String key) {
        StringBuilder binary = new StringBuilder();
        int width = image.getWidth();
        int height = image.getHeight();

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                int blue = pixel & 0xFF;
                int lsb = blue & 1;
                binary.append(lsb);

                // Check for null terminator (00000000)
                if (binary.length() % 8 == 0) {
                    String byteStr = binary.substring(binary.length() - 8);
                    if (Integer.parseInt(byteStr, 2) == 0) {
                        break outerLoop;
                    }
                }
            }
        }

        // Convert binary to encrypted string
        StringBuilder encryptedMessage = new StringBuilder();
        for (int i = 0; i + 8 <= binary.length(); i += 8) {
            String byteStr = binary.substring(i, i + 8);
            int ascii = Integer.parseInt(byteStr, 2);
            if (ascii == 0) break;
            encryptedMessage.append((char) ascii);
        }

        return xorWithKey(encryptedMessage.toString(), key); // Final decoded message
    }

    private String xorWithKey(String data, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            result.append((char) (data.charAt(i) ^ key.charAt(i % key.length())));
        }
        return result.toString();
    }
}
