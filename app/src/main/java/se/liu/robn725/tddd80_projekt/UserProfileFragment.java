package se.liu.robn725.tddd80_projekt;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

/**
 * Handles the user's own profile.
 * Has all of the data relating to the user.
 */
public class UserProfileFragment extends Fragment {
    private UserViewModel userViewModel;
    private TextView username;
    private TextView fullname;
    private TextView email;
    private TextView phone;
    private ImageButton profilePictureButton;
    private TextView followerCount;
    private TextView followingCount;
    private MiscDataViewModel miscDataViewModel;
    private StorageReference storageReference;
    private ProgressBar progressBar;
    private Uri uri;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    public UserProfileFragment() {
        // Required empty public constructor
    }

    public static UserProfileFragment newInstance() {
        return new UserProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        username = view.findViewById(R.id.profile_username_text);
        fullname = view.findViewById(R.id.profile_fullname_text);
        email = view.findViewById(R.id.profile_email_text);
        phone = view.findViewById(R.id.profile_phone_text);
        profilePictureButton = view.findViewById(R.id.profile_change_image_button);
        profilePictureButton.setImageResource(R.drawable.ic_baseline_person_24);
        followerCount = view.findViewById(R.id.profile_followers);
        followingCount = view.findViewById(R.id.profile_following);
        progressBar = view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);

        // Get result from activity
        ActivityResultLauncher<String> getPhoto = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    // OnActivityResult
                    this.uri = uri;
                    // Make sure that if user backs out of picking a image it doesn't crash
                    if (this.uri != null) {
                        uploadProfilePicture(this.uri);
                        profilePictureButton.setImageURI(this.uri);
                        this.uri = null;
                    }

                });

        miscDataViewModel.getIsUploadPhoto().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (miscDataViewModel.getIsUploadPhoto().getValue() != null) {
                    // Launch gallery activity
                    getPhoto.launch("image/*");
                    // Reset the photo mode
                    miscDataViewModel.getIsUploadPhoto().setValue(null);
                }
            }
        });

        miscDataViewModel.getIsCameraPhoto().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (miscDataViewModel.getIsCameraPhoto().getValue() != null) {
                    // Launch camera activity
                    takeCameraPicture();
                    // Reset the photo mode
                    miscDataViewModel.getIsCameraPhoto().setValue(null);
                }

            }
        });

        profilePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a dialog prompting user to upload from camera or gallery
                PictureDialogFragment pictureDialogFragment = PictureDialogFragment.newInstance();
                pictureDialogFragment.show(getParentFragmentManager(), "Choice");

            }
        });

        // Sets the user info to display profile
        setUserInfo();

        return view;
    }

    /**
     * Uploads the image uri to firebase then send a request to get the uri into our database.
     * @param uri Uri of the image uri
     */
    private void uploadProfilePicture(Uri uri) {
        String imageName = UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child("market");
        StorageReference imagesRef = storageReference.child("images/" + imageName);
        progressBar.setVisibility(View.VISIBLE);

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
                        // Set user image
                        userViewModel.setPhotoUrl(uri.toString());
                        progressBar.setVisibility(View.INVISIBLE);
                        // Update the user's profile picture uri in our own database
                        updateFlaskImageUrl();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                // Display progress bar when uploading an image
                double progressProcent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                progressBar.setProgress((int) progressProcent);
            }
        });
    }

    /**
     * Sends a request to the server to update the user's image uri field to the firebase one.
     * Called after a firebase upload is done.
     */
    public void updateFlaskImageUrl() {
        String url = "https://market-app-swe.herokuapp.com/upload_image";
        HashMap<String, String> imageParamsMap = new HashMap<>();
        imageParamsMap.put("photo_url", userViewModel.getProfilePicture().toString());
        JSONObject imageParams = new JSONObject(imageParamsMap);
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest changePhotoRequest = new JsonObjectRequest(Request.Method.POST, url, imageParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

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
        queue.add(changePhotoRequest);
    }


    /**
     * Sets all of the Views in profile to the correct data from server.
     */
    public void setUserInfo() {
        String username = userViewModel.getUserData().getValue().get("username");
        String firstName = userViewModel.getUserData().getValue().get("first_name");
        String lastName = userViewModel.getUserData().getValue().get("last_name");
        String email = userViewModel.getUserData().getValue().get("email");
        String phone = userViewModel.getUserData().getValue().get("phone");
        String followers = userViewModel.getUserData().getValue().get("follower_count");
        String following = userViewModel.getUserData().getValue().get("following_count");
        String firstAndLastName = firstName + " " + lastName;
        this.username.setText(username);
        fullname.setText(firstAndLastName);
        this.email.setText(email);
        this.phone.setText(phone);
        this.followerCount.setText(followers);
        this.followingCount.setText(following);
        setProfilePicture();
    }

    /**
     * Sets the user's profile picture
     */
    public void setProfilePicture() {
        if (userViewModel.getProfilePicture() != null) {
            // Set internet Uri to image bitmap
            Glide.with(getContext()).load(userViewModel.getProfilePicture()).into(profilePictureButton);
        } else {
            profilePictureButton.setImageResource(R.drawable.ic_baseline_person_24);
        }
    }

    /**
     * Launches the camera intent
     */
    private void takeCameraPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    /**
     * Result of activity.
     * @param requestCode Code of the request.
     * @param resultCode Result of the request.
     * @param data Data from the request.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if camera was used.
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                // Transfer bitmap into uri
                Uri fileUri = getImageUri(getContext(), bitmap);
                profilePictureButton.setImageURI(fileUri);
                userViewModel.setPhotoUrl(fileUri.toString());
                uploadProfilePicture(fileUri);
            }
        }
    }

    /**
     * Transforms Bitmap into Uri.
     * @param inContext Context of MainActivity.
     * @param inImage Bitmap of image to turn into uri.
     * @return An uri of the bitmap
     */
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}