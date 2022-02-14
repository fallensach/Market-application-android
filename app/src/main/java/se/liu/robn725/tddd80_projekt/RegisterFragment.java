package se.liu.robn725.tddd80_projekt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static se.liu.robn725.tddd80_projekt.LoginActivity.INFO;
import static se.liu.robn725.tddd80_projekt.LoginActivity.TOKEN;

/**
 * This class handles everything about the user register.
 * It has requests to the database to add user.
 */
public class RegisterFragment extends Fragment {
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText firstnameInput;
    private EditText lastnameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private UserViewModel userViewModel;

    public RegisterFragment() {
        // Required empty public constructor
    }

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        Button registerButton = view.findViewById(R.id.register);
        usernameInput = view.findViewById(R.id.register_username);
        passwordInput = view.findViewById(R.id.register_password);
        firstnameInput = view.findViewById(R.id.register_firstname);
        lastnameInput = view.findViewById(R.id.register_lastname);
        emailInput = view.findViewById(R.id.register_email);
        phoneInput = view.findViewById(R.id.register_phonenumber);
        Button loginButton = view.findViewById(R.id.back_to_login);

        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    register();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToLogin();
            }
        });

        return view;
    }

    /**
     * Sends a register request to the databse and adds it.
     * @throws JSONException Throw error if response is invalid
     */
    public void register() throws JSONException {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = "https://market-app-swe.herokuapp.com/register";
        JSONObject registerParams = new JSONObject();
        registerParams.put("username", usernameInput.getText().toString());
        registerParams.put("password", passwordInput.getText().toString());
        registerParams.put("first_name", firstnameInput.getText().toString());
        registerParams.put("last_name", lastnameInput.getText().toString());
        registerParams.put("email", emailInput.getText().toString());
        registerParams.put("phone", phoneInput.getText().toString());
        registerParams.put("profile_picture_url", "https://i.stack.imgur.com/dr5qp.jpg");

        JsonObjectRequest registerRequest = new JsonObjectRequest(url, registerParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                    text.show();

                    // Check if user was created successfully before redirecting.
                    if (response.get("message").toString().equals("Användare skapad")) {
                        tryLogin();
                    }
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

        queue.add(registerRequest);

    }

    /**
     * Logs the user in and sets the token in the viewModel ready for use.
     * @throws JSONException Throws error if response is invalid.
     */
    public void tryLogin() throws JSONException {
        String loginUrl = "https://market-app-swe.herokuapp.com/login";
        JSONObject loginParams = new JSONObject();
        loginParams.put("username", usernameInput.getText().toString());
        loginParams.put("password", passwordInput.getText().toString());
        String dataUrl = "https://market-app-swe.herokuapp.com/get_user/" + usernameInput.getText().toString();
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        // Request a login
        JsonObjectRequest loginRequest = new JsonObjectRequest(loginUrl, loginParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Add the token if successful
                try {
                    userViewModel.getToken();
                    userViewModel.setToken(response.get("token").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                login();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Error", error.toString());
            }
        });

        // Send a request to get the user data
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
                String response = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject data = new JSONObject();
                try {
                    data = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String message = data.optString("message");
                Toast text = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
                text.show();
            }
        });
        requestQueue.add(loginRequest);
        requestQueue.add(userDataRequest);
    }

    public void login() {
        JSONObject userData = new JSONObject(userViewModel.getUserData().getValue());
        Intent intent = new Intent(getContext(),  MainActivity.class);
        // Send the token and user data into MainActivity for main use.
        intent.putExtra(TOKEN, userViewModel.getToken().getValue());
        intent.putExtra(INFO, String.valueOf(userData));
        startActivity(intent);
        getActivity().finish();
    }

    //Sends the user back to log in
    public void switchToLogin(){
        LoginFragment loginFragment = LoginFragment.newInstance();
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction().replace(R.id.loginActivity, loginFragment);
        fragmentTransaction.addToBackStack("back");
        fragmentTransaction.commit();
    }
}