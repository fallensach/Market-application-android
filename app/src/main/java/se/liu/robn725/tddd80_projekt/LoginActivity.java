package se.liu.robn725.tddd80_projekt;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for login.
 * Creates the view and adds the loginFragment when created.
 */
public class LoginActivity extends AppCompatActivity {
    public static final String TOKEN = "TOKEN";
    public static final String INFO = "INFO";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState == null) {
            // Add loginFragment to the view.
            LoginFragment loginFragment = LoginFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.loginActivity, loginFragment, null).commit();
        }

    }
}