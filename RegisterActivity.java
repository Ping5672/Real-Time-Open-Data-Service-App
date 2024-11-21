package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * RegisterActivity handles the user interface for new user registration.
 * It allows users to input their email and password to create a new account.
 */
public class RegisterActivity extends AppCompatActivity {
    /** EditText for the user to input their email address. */
    private EditText editTextEmail;

    /** EditText for the user to input their desired password. */
    private EditText editTextPassword;

    /** Button to initiate the registration process. */
    private Button buttonReg;

    /** Button to close the registration screen and return to the main activity. */
    private Button buttonClose;

    /** ProgressBar to indicate that the registration process is ongoing. */
    private ProgressBar progressBar;

    /** TextView that, when clicked, navigates the user to the login screen. */
    private TextView textView;

    /** ViewModel that handles the business logic for the registration process. */
    private RegisterViewModel viewModel;

    /**
     * Called when the activity is first created. This method initializes the activity,
     * sets up the user interface, and checks if a user is already logged in.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initializeViews();
        setupListeners();
        setupObservers();

        if (viewModel.isUserLoggedIn()) {
            navigateToMainActivity();
        }
    }

    /**
     * Initializes all the views used in the activity.
     */
    private void initializeViews() {
        editTextEmail = findViewById(R.id.email_field);
        editTextPassword = findViewById(R.id.password_field);
        buttonReg = findViewById(R.id.register_button);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);
        buttonClose = findViewById(R.id.close_button);
    }

    /**
     * Sets up click listeners for the buttons and text views.
     */
    private void setupListeners() {
        buttonClose.setOnClickListener(v -> navigateToMainActivity());
        textView.setOnClickListener(v -> navigateToLoginActivity());
        buttonReg.setOnClickListener(v -> attemptRegistration());
    }

    /**
     * Sets up observers for the ViewModel's LiveData objects.
     */
    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonReg.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getRegistrationSuccessful().observe(this, isSuccessful -> {
            if (isSuccessful) {
                Toast.makeText(RegisterActivity.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }
        });
    }

    /**
     * Attempts to register a new user with the provided credentials.
     * This method validates the input and calls the ViewModel to perform the registration operation.
     */
    private void attemptRegistration() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(RegisterActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(RegisterActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(email, password);
    }

    /**
     * Navigates to the MainActivity.
     * This method creates an Intent to start the MainActivity and finishes the current activity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to the LoginActivity.
     * This method creates an Intent to start the LoginActivity and finishes the current activity.
     */
    private void navigateToLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }
}