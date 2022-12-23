package com.example.test;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.PersistableBundle;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.test.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Runnable startService = () -> {
            Snackbar.make(binding.fab, "Starting service", Snackbar.LENGTH_LONG).show();
            startService(new Intent(this, MyVpnService.class));
        };

        ActivityResultLauncher<Intent> getPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) {
                Snackbar.make(binding.fab, "Permission not granted!", Snackbar.LENGTH_LONG).show();
                return;
            }

            startService.run();
        });

        binding.fab.setOnClickListener(view -> {
            Intent intent = VpnService.prepare(this);
            if (intent != null) {
                getPermission.launch(intent);
                Snackbar.make(view, "Please grant permission", Snackbar.LENGTH_LONG).show();
            } else {
                startService.run();
            }
        });
    }
}