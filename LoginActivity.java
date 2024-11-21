package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * LoginActivity handles the user interface for user authentication.
 * It allows users to input their credentials and attempt to log in.
 */
public class LoginActivity extends AppCompatActivity {
    /**
     * EditText field for the user to input their email address.
     */
    private EditText editTextEmail;

    /**
     * EditText field for the user to input their password.
     */
    private EditText editTextPassword;

    /**
     * Button that the user clicks to initiate the login process.
     */
    private Button buttonLogin;

    /**
     * Button that allows the user to close or exit the login screen.
     */
    private Button buttonClose;

    /**
     * ProgressBar to show loading state during the login process.
     */
    private ProgressBar progressBar;

    /**
     * TextView that, when clicked, allows the user to navigate to the registration screen.
     */
    private TextView textViewRegister;

    /**
     * ViewModel that handles the business logic for the login process.
     */
    private LoginViewModel viewModel;


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
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

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
        buttonLogin = findViewById(R.id.login_button);
        buttonClose = findViewById(R.id.close_button);
        progressBar = findViewById(R.id.progressBar);
        textViewRegister = findViewById(R.id.registerNow);
    }

    /**
     * Sets up click listeners for the buttons and text views.
     */
    private void setupListeners() {
        buttonClose.setOnClickListener(v -> finish());
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            finish();
        });
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Sets up observers for the ViewModel's LiveData objects.
     */
    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonLogin.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoginSuccessful().observe(this, isSuccessful -> {
            if (isSuccessful) {
                Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }
        });
    }

    /**
     * Attempts to log in the user with the provided credentials.
     * This method validates the input and calls the ViewModel to perform the login operation.
     */
    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.login(email, password);
    }

    /**
     * Navigates to the MainActivity after successful login.
     * This method creates an Intent to start the MainActivity and finishes the current activity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}