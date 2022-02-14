package se.liu.robn725.tddd80_projekt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment to handle how to add posts.
 * Takes care of upload of post information and image.
 */
public class AddPostFragment extends Fragment {
    private ImageView postPicture;
    private String category;
    private EditText title;
    private EditText description;
    private EditText price;
    private Button addPostImageButton;
    private LocationManager locationManager;
    private String currentLocation;
    public static final int PERMISSION_LOCATION_REQUEST = 97;
    private Geocoder geocoder;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private UserViewModel userViewModel;
    private CheckBox locationCheckBox;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private Uri uri;
    public AddPostFragment() {
        // Required empty public constructor
    }

    public static AddPostFragment newInstance() {
        return new AddPostFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_post, container, false);
        Spinner categorySelector = (Spinner) view.findViewById(R.id.category_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySelector.setAdapter(adapter);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        category = (String) categorySelector.getSelectedItem();
        title = view.findViewById(R.id.post_title);
        description = view.findViewById(R.id.post_description);
        price = view.findViewById(R.id.post_price);
        locationCheckBox = view.findViewById(R.id.location_check_box);
        addPostImageButton = view.findViewById(R.id.post_upload_image_button);
        postPicture = view.findViewById(R.id.post_image);
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        ActivityResultLauncher<String> getPostPhoto = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    // OnActivityResult
                    this.uri = uri;
                    postPicture.setImageURI(uri);
                });

        // If user checks to use current location, send a permission request
        locationCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( ((CheckBox)v).isChecked()) {
                    checkLocationPermission();
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Check that current location is not null
                            if (location == null) {
                                currentLocation = "";
                            } else {
                                try {
                                    // Get the current location
                                    Address adress = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
                                    currentLocation = adress.getAddressLine(0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }
        });

        // Get the current live model shared with the fragments
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);

        Button postButton = view.findViewById(R.id.post_add_post);

        // Add a post when clicked on the add post button
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add post to server
                if (uri == null) {
                    Toast.makeText(getContext(), "Vänligen lägg till en bild", Toast.LENGTH_SHORT).show();
                } else {
                    uploadPostPhoto(uri);
                }
            }
        });

        addPostImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPostPhoto.launch("image/*");
            }
        });

        return view;
    }

    /**
     * Sends a POST request to the server with a JSON body and JWT token header.
     * @throws JSONException
     */
    public void addPost() throws JSONException {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = "https://market-app-swe.herokuapp.com/create_post";
        JSONObject postParams = new JSONObject();
        postParams.put("picture", uri);
        postParams.put("category", category);
        postParams.put("title", title.getText().toString());
        postParams.put("description", description.getText().toString());
        postParams.put("price", price.getText().toString());

        // check if location is null before proceeding
        if (currentLocation == null) {
            postParams.put("location", "");
        } else {
            postParams.put("location", currentLocation);
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(url, postParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Success", response.toString());
                NavController navController = Navigation.findNavController(getView());
                navController.navigate(R.id.action_addPostFragment_to_homeFragment);
                try {
                    Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                    text.show();

                } catch (JSONException e) {
                    try {
                        Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                        text.show();
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }

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


        })
        {
            // Send header with token
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Authorization", "Bearer " + userViewModel.getToken().getValue());
                return params;
            }
        };

        queue.add(postRequest);

    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        } else {
            // Ask for permission
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST);

        }
    }

    /**
     * Sends a upload request to the server.
     * Uploads the image then calls on addPost so the user doesn't upload
     * before the image is uploaded.
     * @param uri Uri of the post image.
     */
    public void uploadPostPhoto(Uri uri) {
        // Generate an image name.
        String imageName = UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child("market");
        StorageReference imagesRef = storageReference.child("images/" + imageName);

        // While the file names are the same, the references point to different files
        imageRef.getName().equals(imagesRef.getName());    // true
        imageRef.getPath().equals(imagesRef.getPath());    // false
        imagesRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getContext(), "Uploaded", Toast.LENGTH_SHORT).show();
                // Get the path to the image
                imagesRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        setUri(uri);
                        try {
                            addPost();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println(e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}







