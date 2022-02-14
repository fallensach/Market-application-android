package se.liu.robn725.tddd80_projekt;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for a dialog prompting user to user camera or gallery.
 */
public class PictureDialogFragment extends DialogFragment {
    private MiscDataViewModel miscDataViewModel;
    private static final int CAMERA_REQUEST_CODE = 99;
    private static final int PHOTO_REQUEST_CODE = 98;
    public PictureDialogFragment() {
    }

    public static PictureDialogFragment newInstance() {
        return new PictureDialogFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        miscDataViewModel.getIsUploadPhoto();
        miscDataViewModel.getIsCameraPhoto();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Ladda upp bild").setItems(R.array.camera_or_gallery, new DialogInterface.OnClickListener() {

            /**
             * Dialog to show user to pick between camera or gallery
             * @item the index of the item that was chosen
             */
            @Override
            public void onClick(DialogInterface dialog, int item) {
                // If user chose camera
                if (item == 0) {
                    checkCameraPermission();
                // If user chose gallery
                } else if (item == 1) {
                    checkStoragePermission();
                }
            }
        });
        return builder.create();
    }

    public void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
                miscDataViewModel.setIsUploadPhoto(true);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PHOTO_REQUEST_CODE);
        }
    }

    public void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            miscDataViewModel.setIsCameraPhoto(true);

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

}

