package com.example.skypeclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.contentcapture.DataRemovalRequest;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

public class VideoCallActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener{

    private static String API_Key="46762542";
    private static String SESSION_ID="2_MX40Njc2MjU0Mn5-MTU5MDQ5MzQ2ODQyMn4rcm1ISUlJWmpxeVpxMytJS0JVVThkRWl-fg";
    private static String TOKEN="T1==cGFydG5lcl9pZD00Njc2MjU0MiZzaWc9YjczYzAzMjY0ZDNjM2JmNjMyMzZiMDE5M2U2MDkxZDk3MmZhNDVmZjpzZXNzaW9uX2lkPTJfTVg0ME5qYzJNalUwTW41LU1UVTVNRFE1TXpRMk9EUXlNbjRyY20xSVNVbEpXbXB4ZVZweE15dEpTMEpWVlRoa1JXbC1mZyZjcmVhdGVfdGltZT0xNTkwNDkzNTQ1Jm5vbmNlPTAuNDA1ODc5OTcwNzU0NjYyMSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTkzMDg1NTQ1JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG=VideoCallActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM=124;
    private ImageView closeVideoChatBtn;
    private DatabaseReference usersRef;
    private String userID="";

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        userID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        closeVideoChatBtn=findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(userID).hasChild("Ringing")){
                            usersRef.child(userID).child("Ringing").removeValue();
                            if (mPublisher!=null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber!=null){
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }
                        if (dataSnapshot.child(userID).hasChild("Calling")){
                            usersRef.child(userID).child("Calling").removeValue();
                            if (mPublisher!=null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber!=null){
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }
                        else {
                            if (mPublisher!=null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber!=null){
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        });
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoCallActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String[] perms={Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO};
        if(EasyPermissions.hasPermissions(this,perms)){
            mPublisherViewController=findViewById(R.id.publisher_container);
            mSubscriberViewController=findViewById(R.id.subscriber_container);

            //Initialize and connect to the session.
            mSession=new Session.Builder(this,API_Key,SESSION_ID).build();
            mSession.setSessionListener(VideoCallActivity.this);
            mSession.connect(TOKEN);
        }
        else{
            EasyPermissions.requestPermissions(this, "Hey this App needs Mic and Camera Permissions. Please Allow.", RC_VIDEO_APP_PERM,perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    //publishing a stream to a session
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        mPublisher=new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoCallActivity.this);

        mPublisherViewController.addView(mPublisher.getView());
        if (mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Stream Disconnected.");

    }

    //Subscribing to the Streams.
    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received.");

        if (mSubscriber==null){
            mSubscriber=new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped.");
        if (mSubscriber==null){
            mSubscriber=null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error.");
    }
}
