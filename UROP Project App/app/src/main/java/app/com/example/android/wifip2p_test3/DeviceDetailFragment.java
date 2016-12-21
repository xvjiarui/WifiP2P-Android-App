

package app.com.example.android.wifip2p_test3;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import app.com.example.android.wifip2p_test3.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    public static int PORT = 8088;
    private static final int READ_REQUEST_CODE = 1337;
    static final String readyRxHeader="*#06#xvjiarui0826*#06#";

    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    public Server server = null;
    public Client client = null;

    public TextView message_display = null;
    public EditText message_editText = null;
    public TextView fileInfo_display = null;

    public Uri fileUri = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        mContentView = inflater.inflate(R.layout.device_detail, null);

        message_display = (TextView) mContentView.findViewById(R.id.message_display);
        message_editText = (EditText) mContentView.findViewById(R.id.message_editText);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true);
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                        mContentView.findViewById(R.id.message_panel).setVisibility(View.GONE);
                        resetViews();
                    }
                });

        mContentView.findViewById(R.id.btn_choose_file).setOnClickListener(
                new View.OnClickListener() {
                    @Override

                    public void onClick(View v) {
                        Thread thread = new Thread(new ImageThread());
                        thread.start();
                    }
                }
        );

        mContentView.findViewById(R.id.btn_send_file).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (fileUri!=null) {
                            String filenameArray[] = (Utils.getPath(getActivity(), fileUri)).split("/");
                            String fileName=filenameArray[filenameArray.length-1];
                            if (info.isGroupOwner){
                                server.sendHeader(readyRxHeader,fileName);
                                try {
                                    TimeUnit.SECONDS.sleep(2);
                                    Log.v(MainActivity.TAG,"Sleep");
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                                server.sendFile(Utils.getPath(getActivity(), fileUri));
                                Toast.makeText(getActivity(), "Server_sendFile", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                client.sendHeader(readyRxHeader,fileName);
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                                client.sendFile(Utils.getPath(getActivity(), fileUri));
                                Toast.makeText(getActivity(), "Client_sendFile", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else Toast.makeText(getActivity(),"Please select a file.",Toast.LENGTH_SHORT).show();
                    }
                });

        mContentView.findViewById(R.id.btn_send).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (info.isGroupOwner){
                            server.sendMessage();
                            Toast.makeText(getActivity(), "Server_sent", Toast.LENGTH_SHORT).show();
                        }

                        else {
                            client.sendMessage();
                            Toast.makeText(getActivity(), "Client_sent", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            if (data != null) {
                fileUri = data.getData();
                showImage(fileUri);
                Toast.makeText(getActivity(), "File select.", Toast.LENGTH_SHORT).show();
            }


        }
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        //this.getView().setVisibility(View.VISIBLE);
        CircularAnimUtil.show(this.getView());

        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        CircularAnimUtil.show(mContentView.findViewById(R.id.btn_send_file));
        CircularAnimUtil.show(mContentView.findViewById(R.id.btn_choose_file));
        CircularAnimUtil.hide(mContentView.findViewById(R.id.btn_connect));
        CircularAnimUtil.show(mContentView.findViewById(R.id.message_panel));
        CircularAnimUtil.show(message_display);
        setupAll();
    }


    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        //this.getView().setVisibility(View.VISIBLE);
        CircularAnimUtil.show(this.getView());
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }


    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_send_file).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_choose_file).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public void setupAll(){
        if (info.isGroupOwner){
            if (server==null){
                try {
                    TimeUnit.SECONDS.sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
               server= new Server((MainActivity) getActivity());
                Toast.makeText(getActivity(), "Server", Toast.LENGTH_SHORT).show();
            }
        }

        else {
            if (client==null){
                try {
                    TimeUnit.SECONDS.sleep(2);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                client=new Client((MainActivity) getActivity(),info.groupOwnerAddress.getHostAddress().toString(), PORT);
                Toast.makeText(getActivity(), "Client", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void destroy() {
        if (info.isGroupOwner) {
            if (server != null) {
                server.onDestroy();
            }
        } else {
            if (client!= null) {
                client.onDestroy();
            }
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public class ImageThread implements Runnable {

        public void run() {
            try {
                performFileSearch();
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Crash", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void performFileSearch() {

        // BEGIN_INCLUDE (use_open_document_intent)
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a file (as opposed to a list
        // of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers, it would be
        // "*/*".
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
        // END_INCLUDE (use_open_document_intent)
    }


    public void showImage(Uri uri) {
        // BEGIN_INCLUDE (create_show_image_dialog)
        if (uri != null) {
            // Since the URI is to an image, create and show a DialogFragment to display the
            // image to the user.
            FragmentManager fm = getActivity().getFragmentManager();
            ImageDialogFragment imageDialog = new ImageDialogFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putParcelable("URI", uri);
            imageDialog.setArguments(fragmentArguments);
            imageDialog.show(fm, "image_dialog");
        }
        // END_INCLUDE (create_show_image_dialog)
    }


    /**
     * DialogFragment which displays an image, given a URI.
     */
    public static class ImageDialogFragment extends DialogFragment {
        private Dialog mDialog;
        private Uri mUri;
        public String imageInfo = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUri = getArguments().getParcelable("URI");
        }

        /**
         * Create a Bitmap from the URI for that image and return it.
         *
         * @param uri the Uri for the image to return.
         */
        private Bitmap getBitmapFromUri(Uri uri) {
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor =
                        getActivity().getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                return image;
            } catch (Exception e) {
                imageInfo += e.toString();
                //Toast.makeText(getActivity(),"Failed to load image.",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (parcelFileDescriptor != null) {
                        parcelFileDescriptor.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    imageInfo += e.toString();
                    //Toast.makeText(getActivity(),"Error closing ParcelFile Descriptor",Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mDialog = super.onCreateDialog(savedInstanceState);
            // To optimize for the "lightbox" style layout.  Since we're not actually displaying a
            // title, remove the bar along the top of the fragment where a dialog title would
            // normally go.
            mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            final ImageView imageView = new ImageView(getActivity());
            mDialog.setContentView(imageView);

            // BEGIN_INCLUDE (show_image)
            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread.  Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask.  The doInBackground
            // method will run in a separate thread, but onPostExecute will run in the main
            // UI thread.
            AsyncTask<Uri, Void, Bitmap> imageLoadAsyncTask = new AsyncTask<Uri, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Uri... uris) {
                    dumpImageMetaData(uris[0]);
                    return getBitmapFromUri(uris[0]);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            };
            imageLoadAsyncTask.execute(mUri);
            // END_INCLUDE (show_image)

            return mDialog;
        }

        @Override
        public void onStop() {
            super.onStop();
            if (getDialog() != null) {
                getDialog().dismiss();
            }
        }

        /**
         * Grabs metadata for a document specified by URI, logs it to the screen.
         *
         * @param uri The uri for the document whose metadata should be printed.
         */
        public void dumpImageMetaData(Uri uri) {
            // BEGIN_INCLUDE (dump_metadata)

            // The query, since it only applies to a single document, will only return one row.
            // no need to filter, sort, or select fields, since we want all fields for one
            // document.
            Cursor cursor = getActivity().getContentResolver()
                    .query(uri, null, null, null, null, null);

            try {
                // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursor != null && cursor.moveToFirst()) {

                    // Note it's called "Display Name".  This is provider-specific, and
                    // might not necessarily be the file name.
                    String displayName = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    //Toast.makeText(getActivity(),"Display Name: "+ displayName, Toast.LENGTH_SHORT).show();
                    imageInfo += "Name:" + displayName;
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    // If the size is unknown, the value stored is null.  But since an int can't be
                    // null in java, the behavior is implementation-specific, which is just a fancy
                    // term for "unpredictable".  So as a rule, check if it's null before assigning
                    // to an int.  This will happen often:  The storage API allows for remote
                    // files, whose size might not be locally known.
                    String size = null;
                    if (!cursor.isNull(sizeIndex)) {
                        // Technically the column stores an int, but cursor.getString will do the
                        // conversion automatically.
                        size = cursor.getString(sizeIndex);
                    } else {
                        size = "Unknown";
                    }
                    imageInfo += "Size: " + size;
                    //Toast.makeText(getActivity(),"Size: "+size,Toast.LENGTH_SHORT).show();
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            // END_INCLUDE (dump_metadata)
        }
    }


}
