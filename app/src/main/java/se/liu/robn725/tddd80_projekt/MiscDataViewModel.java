package se.liu.robn725.tddd80_projekt;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Integer.parseInt;

/**
 * A ViewModel for misc. data.
 * The data stored here is temporary and is used for sending quick data over fragments that can be replaced whenever.
 */
public class MiscDataViewModel extends ViewModel {
    private MutableLiveData<Boolean> isUploadPhoto;
    private MutableLiveData<Boolean> isCameraPhoto;
    private MutableLiveData<Integer> postId;
    private MutableLiveData<HashMap<String, String>> searchedUserData;
    private MutableLiveData<ArrayList<Post>> searchedUserPosts;
    private boolean isProfile;
    
    public MutableLiveData<Boolean> getIsUploadPhoto() {
        if (isUploadPhoto == null) {
            isUploadPhoto = new MutableLiveData<>();
        }
        return isUploadPhoto;
    }

    public MutableLiveData<Boolean> getIsCameraPhoto() {
        if (isCameraPhoto == null) {
            isCameraPhoto = new MutableLiveData<>();
        }
        return isCameraPhoto;
    }

    public void setIsCameraPhoto(boolean bool) {
        isCameraPhoto.setValue(bool);
    }

    public void setIsUploadPhoto(boolean bool) {
        isUploadPhoto.setValue(bool);
    }

    public MutableLiveData<Integer> getPostId() {
        if (postId == null) {
            postId = new MutableLiveData<>();
        }
        return postId;
    }
    
    public void setPostId(int id) {
        postId.setValue(id);
    }
    

    public MutableLiveData<HashMap<String, String>> getSearchedUserData() {
        if (searchedUserData == null) {
            searchedUserData = new MutableLiveData<>();
        }
        return searchedUserData;
    }


    public MutableLiveData<ArrayList<Post>> getUserPosts() {
        if (searchedUserPosts == null) {
            searchedUserPosts = new MutableLiveData<>();
        }
        return searchedUserPosts;
    }

    /**
     * Takes a JSONArray with valid types and converts it into a ArrayList filled with Post objects
     * @param userPostsAsJSONArray JSONArray with types: id, owner_id, title, description, category, price, time, location, picture, likes.
     * @throws JSONException Throws error if response is invalid.
     */
    public void setUserPosts(JSONArray userPostsAsJSONArray) throws JSONException {
        ArrayList<Post> posts = new ArrayList<>();
        for (int i = 0; i < userPostsAsJSONArray.length(); i++) {
            JSONObject postObject = (JSONObject) userPostsAsJSONArray.get(i);
            int postId = parseInt(postObject.get("id").toString());
            String ownerId = postObject.get("owner_id").toString();
            String title = postObject.get("title").toString();
            String description = postObject.get("description").toString();
            String category = postObject.get("category").toString();
            int price = parseInt(postObject.get("price").toString());
            String time = postObject.get("time").toString();
            String location = postObject.get("location").toString();
            String pictureUrl = postObject.get("picture").toString();
            int likes = parseInt(postObject.get("likes").toString());

            Post post = new Post(postId, ownerId, title
                    , description, category, price, time
                    , location, pictureUrl, likes);
            posts.add(post);
        }
        searchedUserPosts.setValue(posts);
    }

    /**
     * Takes a JSONObject response and converts it into the mutableLiveData version.
     * @param userObject JSONObject with information about the user.
     * @throws JSONException Throw error if response is invalid.
     */
    public void setUserData(JSONObject userObject) throws JSONException {
        HashMap<String, String> userDataMap = new HashMap<>();
        userDataMap.put("username", userObject.get("username").toString());
        userDataMap.put("first_name", userObject.get("first_name").toString());
        userDataMap.put("last_name", userObject.get("last_name").toString());
        userDataMap.put("email", userObject.get("email").toString());
        userDataMap.put("phone", userObject.get("phone").toString());
        userDataMap.put("follower_count", userObject.get("follower_count").toString());
        userDataMap.put("following_count", userObject.get("following_count").toString());
        if (!userObject.isNull("profile_picture_url")) {
            userDataMap.put("profile_picture_url", userObject.get("profile_picture_url").toString());
        }

        searchedUserData.setValue(userDataMap);
    }
    

    public String getUsername() {
        if (searchedUserData == null) {
            return null;
        }
        return searchedUserData.getValue().get("username");
    }

    public String getFirstName() {
        return searchedUserData.getValue().get("first_name");
    }

    public String getLastName() {
        return searchedUserData.getValue().get("last_name");
    }

    public String getPhone() {
        return searchedUserData.getValue().get("phone");
    }

    public String getEmail() {
        return searchedUserData.getValue().get("email");
    }

    public Uri getProfilePicture() {
        if (searchedUserData.getValue().get("profile_picture_url") != null) {
            return Uri.parse(searchedUserData.getValue().get("profile_picture_url"));
        }
        return null;
    }
    
    public boolean isProfile() {
        return isProfile;
    }
    
    public void setIsProfile(boolean b) {
        isProfile = b;
    }
    
}
