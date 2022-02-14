package se.liu.robn725.tddd80_projekt;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static androidx.navigation.ui.NavigationUI.setupActionBarWithNavController;

/**
 * MainActivity creates the navigation and replaces the nav_host_fragment when called on.
 * On login the MainActivity also sets all of the users data to the UserViewModel ready for use in other fragments.
 */
public class MainActivity extends AppCompatActivity {
    private String userDataString;
    private JSONObject userDataJSON = new JSONObject();
    private UserViewModel userViewModel;
    private MiscDataViewModel miscDataViewModel;
    private static final int CAMERA_REQUEST_CODE = 99;
    private static final int PHOTO_REQUEST_CODE = 98;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(this).get(MiscDataViewModel.class);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        // Start intent
        Intent intent = getIntent();
        // Get the token from LoginActivity
        String token = intent.getStringExtra(LoginActivity.TOKEN);
        userDataString = intent.getStringExtra(LoginActivity.INFO);
        miscDataViewModel.getIsCameraPhoto();
        miscDataViewModel.getIsUploadPhoto();

        try {
            // Make the userDataString into a JSONObject
            userDataJSON = new JSONObject(userDataString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Create and set the token to our viewModel
        userViewModel.getToken();
        userViewModel.setToken(token);

        try {
            // Create and set the user data to our viewModel
            userViewModel.getUserData();
            userViewModel.setUserData(userDataJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Get navHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        // Create navigation bar
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Listener to navigation bar destinations
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                // Set isProfile to remove like button if own post.
                if (destination.getId() == R.id.profileFragment) {
                    miscDataViewModel.setIsProfile(true);
                } else if (destination.getId() == R.id.searchedUserProfile) {
                    miscDataViewModel.setIsProfile(false);
                } else if (destination.getId() == R.id.homeFragment) {
                    miscDataViewModel.setIsProfile(false);
                }
            }
        });

        if (savedInstanceState == null) {
            HomeFragment homeFragment = HomeFragment.newInstance();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Add actions to the toolbar
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Send a DELETE request with JSON token header to the server to log out.
     */
    public void logout() {
        String url = "https://market-app-swe.herokuapp.com/logout";
        HashMap<String, String> logoutParamsMap = userViewModel.getTokenHeader();
        JSONObject logoutParams = new JSONObject(logoutParamsMap);
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest logoutRequest = new JsonObjectRequest(Request.Method.DELETE, url, logoutParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Message", response.toString());

                // Make token null, invalid.
                userViewModel.destroyToken();
                switchToLogin();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Error", error.toString());
            }
        }) {
            // Send header with token
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Authorization", "Bearer " + userViewModel.getToken().getValue());
                return params;
            }

        };
        queue.add(logoutRequest);
    }
    // Sends the user back to Log in and sends them a log out message
    public void switchToLogin() {
        Intent intent = new Intent(this,  LoginActivity.class);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Utloggad", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                miscDataViewModel.setIsCameraPhoto(true);
            }
        } else if (requestCode == PHOTO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                miscDataViewModel.setIsUploadPhoto(true);
            }
        }
    }
}