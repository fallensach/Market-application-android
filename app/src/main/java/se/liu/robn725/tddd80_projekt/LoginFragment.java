package se.liu.robn725.tddd80_projekt;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import static se.liu.robn725.tddd80_projekt.LoginActivity.INFO;
import static se.liu.robn725.tddd80_projekt.LoginActivity.TOKEN;

/**
 * LoginFragment handles all of the logins.
 * It sets the user data and token when logged in and sends it to MainActivity as an extra intent.
 */
public class LoginFragment extends Fragment {
    private EditText usernameBox;
    private EditText passwordBox;
    private RequestQueue requestQueue;
    private UserViewModel userViewModel;
    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestQueue = Volley.newRequestQueue(getContext());
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);

        Button login_button = view.findViewById(R.id.login_button);
        Button register_button = view.findViewById(R.id.register_button);
        usernameBox = view.findViewById(R.id.login_username);
        passwordBox = view.findViewById(R.id.login_password);

        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToRegister();
            }
        });

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tryLogin();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    /**
     * Sends a login request to the server.
     * Gets back a response containing user data.
     * @throws JSONException Throw error if server responds badly.
     */
    public void tryLogin() throws JSONException {
        String loginUrl = "https://market-app-swe.herokuapp.com/login";
        JSONObject loginParams = new JSONObject();
        loginParams.put("username", usernameBox.getText().toString());
        loginParams.put("password", passwordBox.getText().toString());
        String dataUrl = "https://market-app-swe.herokuapp.com/get_user/" + usernameBox.getText().toString();

        JsonObjectRequest loginRequest = new JsonObjectRequest(loginUrl, loginParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Set token
                    userViewModel.getToken();
                    userViewModel.setToken(response.get("token").toString());
                    login();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Error", error.toString());
            }
        });

        JsonObjectRequest userDataRequest = new JsonObjectRequest(Request.Method.GET, dataUrl,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Success", String.valueOf(response));
                // Set the user data in the live model when logging in
                try {
                    userViewModel.getUserData();
                    userViewModel.setUserData(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
        requestQueue.add(loginRequest);
        requestQueue.add(userDataRequest);
    }

    public void switchToRegister() {
        RegisterFragment registerFragment = RegisterFragment.newInstance();
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction().replace(R.id.loginActivity, registerFragment);
        fragmentTransaction.addToBackStack("back");
        fragmentTransaction.commit();
    }

    public void login() {
        JSONObject userData = new JSONObject(userViewModel.getUserData().getValue());
        Intent intent = new Intent(getContext(),  MainActivity.class);
        // Add token to intent
        intent.putExtra(TOKEN, userViewModel.getToken().getValue());
        // Add user data into intent
        intent.putExtra(INFO, String.valueOf(userData));
        // Start new activity and send the extras
        startActivity(intent);
        getActivity().finish();
    }
}