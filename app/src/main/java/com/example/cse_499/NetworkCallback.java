package com.example.cse_499;

public class NetworkCallback {
    interface onImageResponseReceived {
        public void onReceived(int code, UploadFileResponse response);
    }

}
