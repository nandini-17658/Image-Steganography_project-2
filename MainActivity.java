package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure you are using the correct IDs from XML
        Button encodeButton = findViewById(R.id.encode_button);
        Button decodeButton = findViewById(R.id.decode_button);

        // Set click listener for encode button
        encodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the Encode activity when the button is clicked
                Intent intent = new Intent(MainActivity.this, Encode.class);
                startActivity(intent);
            }
        });

        // Set click listener for decode button
        decodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the Decode activity when the button is clicked
                Intent intent = new Intent(MainActivity.this, Decode.class);
                startActivity(intent);
            }
        });
    }
}
