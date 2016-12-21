package app.com.example.android.wifip2p_test3;

/**
 * Created by Scary on 7/16/16.
 */

import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    String dstAddress;
    int dstPort;
    TextView message_display;
    EditText message_editText;
    Handler updateConversationHandler;
    MainActivity mainActivity;
    boolean chatting = true;

    public static Socket socket = null;
    static final int socketServerPORT = 8088;
    static final String messageFromServer = "Server Says: ";
    static final String messageFromClient = "Client Says: ";
    static final String fileFromServer = "Server Sends: ";
    static final String fileFromClient = "Client Sends: ";

    static final String readyRxHeader = "*#06#xvjiarui0826*#06#";
    static final String separate = "/";
    static final String notRxHeader = "*#07#xvjiarui0826*#07#";

    Client(MainActivity mainActivity, String dstAddress, int dstport) {
        this.mainActivity = mainActivity;
        this.dstAddress = dstAddress;
        this.dstPort = dstport;
        message_display = (TextView) mainActivity.findViewById(R.id.message_display);
        message_editText = (EditText) mainActivity.findViewById(R.id.message_editText);
        updateConversationHandler = new Handler();
        new Thread(new ClientThread()).start();
    }


    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                socket = new Socket(dstAddress, dstPort);
                CommunicationThread commThread = new CommunicationThread(socket);
                new Thread(commThread).start();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket serverSocket;
        private BufferedReader input;

        public CommunicationThread(Socket serverSocket) {
            this.serverSocket = serverSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.serverSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            String fileName = "Unknown";
            String message = "Error";
            while (!Thread.currentThread().isInterrupted() && chatting) {
                try {
                    String read = input.readLine();
                    if (read == null) {
                        serverSocket.close();
                        return;
                    }
                    String inputMessage = read;
                    Log.v(MainActivity.TAG, inputMessage);
                    String inputArray[] = read.split(separate);
                    Log.v(MainActivity.TAG, inputArray[inputArray.length - 1]);
                    //Log.v(MainActivity.TAG,inputArray[inputArray.length-2]);
                    if (inputArray[inputArray.length - 2].contains(readyRxHeader)) {
                        fileName = inputArray[inputArray.length - 1];
                        Log.v(MainActivity.TAG, "exit chatting");
                        chatting = false;
                        updateConversationHandler.post(new updateUIThreadFromServerFile(fileName));
                    } else {
                        message = inputArray[inputArray.length - 1];
                        updateConversationHandler.post(new updateUIThreadFromServer(message));
                    }
                    /**
                     if (read.contains(readyRxHeader)){
                     read=read.replace(readyRxHeader, "");
                     read=read.replace(separate,"");
                     fileName=read;
                     Log.v(MainActivity.TAG,fileName);
                     chatting=false;
                     }*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while (!chatting) {
                ClientRxThread clientRxThread = new ClientRxThread(serverSocket, fileName);
                Log.v(MainActivity.TAG, "delaying");
                clientRxThread.run();
                chatting = true;
                CommunicationThread commThread = new CommunicationThread(socket);
                new Thread(commThread).start();
            }
        }
    }

    class updateUIThreadFromServer implements Runnable {

        private String msg;

        public updateUIThreadFromServer(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            String pre_message = message_display.getText().toString();
            msg = pre_message + messageFromServer + msg + "\n";
            message_display.setText(msg);
            message_editText.setText("");
        }
    }

    class updateUIThreadFromClient implements Runnable {

        private String msg;

        public updateUIThreadFromClient(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            String pre_message = message_display.getText().toString();
            msg = pre_message + messageFromClient + msg + "\n";
            message_display.setText(msg);
            message_editText.setText("");
        }
    }

    class updateUIThreadFromServerFile implements Runnable {

        private String msg;

        public updateUIThreadFromServerFile(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            String pre_message = message_display.getText().toString();
            msg = pre_message + fileFromServer + msg + "\n";
            message_display.setText(msg);
        }
    }

    class updateUIThreadFromClientFile implements Runnable {

        private String msg;

        public updateUIThreadFromClientFile(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            String pre_message = message_display.getText().toString();
            msg = pre_message + fileFromClient + msg + "\n";
            message_display.setText(msg);
        }
    }

    class updateImageThread implements Runnable {
        File file;

        updateImageThread(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            showImage(Uri.fromFile(file));
        }

    }

    public class ClientTxThread implements Runnable {
        Socket socket;
        String filePath;

        public ClientTxThread(Socket socket, String filePath) {
            this.socket = socket;
            this.filePath = filePath;

        }

        @Override
        public void run() {
            File file = new File(filePath);
            String fileName = "";
            String inputArray[] = filePath.split("/");
            fileName = inputArray[inputArray.length - 1];
            //Log.d(MainActivity.TAG,uri.getPath().toString());
            Log.d(MainActivity.TAG, filePath);
            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(bytes, 0, bytes.length);

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(bytes);
                oos.flush();
                oos.reset();
                //socket.close();

                final String sentMsg = "Finished";
                updateConversationHandler.post(new updateUIThreadFromServer(sentMsg));
                //sendFileName(fileName);

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } /*finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }*/

        }
    }

    public class ClientRxThread implements Runnable {

        private Socket serverSocket;
        private String fileName;

        public ClientRxThread(Socket serverSocket, String fileName) {
            this.serverSocket = serverSocket;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WifiP2p Download";
                File dir = new File(fullPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(fullPath, "WifiP2p-" + fileName);
                ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());
                byte[] bytes;
                FileOutputStream fos = null;
                try {
                    bytes = (byte[]) ois.readObject();
                    fos = new FileOutputStream(file);
                    fos.write(bytes);
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                updateConversationHandler.post(new updateUIThreadFromServer("Finished"));
                updateConversationHandler.post(new updateImageThread(file));

            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                updateConversationHandler.post(new updateUIThreadFromServer(eMsg));

            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                final String eMsg = "Something wrong: " + e.getMessage();
                updateConversationHandler.post(new updateUIThreadFromServer(eMsg));
            }
        }
    }


    public void sendMessage() {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
            String msg = message_editText.getText().toString();
            String txmsg = notRxHeader + separate + msg;
            out.println(txmsg);
            updateConversationHandler.post(new updateUIThreadFromClient(msg));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendFile(String filePath) {
        Thread sendFile = new Thread(new ClientTxThread(socket, filePath));
        sendFile.start();
    }

    public void sendHeader(String header, String fileName) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
            String msg = header + separate + fileName;
            out.println(msg);
            updateConversationHandler.post(new updateUIThreadFromClientFile(fileName));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showImage(Uri uri) {
        // BEGIN_INCLUDE (create_show_image_dialog)
        if (uri != null) {
            // Since the URI is to an image, create and show a DialogFragment to display the
            // image to the user.
            FragmentManager fm = mainActivity.getFragmentManager();
            DeviceDetailFragment.ImageDialogFragment imageDialog = new DeviceDetailFragment.ImageDialogFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putParcelable("URI", uri);
            imageDialog.setArguments(fragmentArguments);
            imageDialog.show(fm, "image_dialog");
        }
        // END_INCLUDE (create_show_image_dialog)
    }

    public void onDestroy() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}