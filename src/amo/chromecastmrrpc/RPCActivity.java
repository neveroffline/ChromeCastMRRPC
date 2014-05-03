/*
 * Copyright 2013/2014 Dustin D. Brand - google.com/+DustinBrand
 * 
 * 
The content of this project itself is licensed under the 
Creative Commons Attribution 3.0 license, and the underlying 
source code used to format and display that content is 
licensed under the MIT license.

http://creativecommons.org/licenses/by/3.0/us/deed.en_US
http://opensource.org/licenses/mit-license.php
 */

package amo.chromecastmrrpc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.RemotePlaybackClient;
import android.support.v7.media.RemotePlaybackClient.ItemActionCallback;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class RPCActivity extends Activity {

    ListView routeList;
    public MediaRouter.RouteInfo curRoute;
    public boolean ccSelected = false;
    public RemotePlaybackClient mRemotePlaybackClient;	
    public MediaRouter router;
    public MediaRouteSelector selector;
    // replace below with w/e you want to test...
    public Uri videoUri = Uri.parse("http://archive.org/download/Sintel/sintel-2048-stereo_512kb.mp4");
    ArrayAdapter<RouteLabelWrapper> adapter;
    TextView txtStatus;
    
    
    // show the route name in the adapter
    private static class RouteLabelWrapper {
        MediaRouter.RouteInfo routeInfo;
        public RouteLabelWrapper(MediaRouter.RouteInfo routeInfo) {
            this.routeInfo = routeInfo;
        }

        @Override
        public String toString() {
            return routeInfo.getName();
        	//routeInfo.
        }
        
        public String getID() {
        	return routeInfo.getId();
        }
    }

    // handle route callbacks, and add them to the adapter.
    MediaRouter.Callback callback = new MediaRouter.Callback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteAdded(router, route);
            if (route.isDefault())
                return;
            
            System.out.println("Adding: " + route.getName());
            adapter.add(new RouteLabelWrapper(route));
        }
        
        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
        	// TODO Auto-generated method stub
        	super.onRouteRemoved(router, route);
        	System.out.println("Removing: " + route.getName());
        	adapter.remove(new RouteLabelWrapper(route));
        }
        
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
        	System.out.println("Selecting: " + route.getName());
        	super.onRouteSelected(router, route);
        }
        
        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
        	// TODO Auto-generated method stub
        	System.out.println("Unselecting: " + route.getName());
        	super.onRouteUnselected(router, route);
        }
        
        
    };

    // mrCRC
    MediaRouter.ControlRequestCallback mrCRC = new MediaRouter.ControlRequestCallback() {
    	@Override
    	public void onError(String error, Bundle data) {
    		// TODO Auto-generated method stub
    		super.onError(error, data);
    		if(error == null || data == null) {
        		System.out.println("Null error");
    		} else {
        		System.out.println(error);
    		}
    	}
    	
    	@Override
    	public void onResult(Bundle data) {
    		// TODO Auto-generated method stub
    		super.onResult(data);
    		System.out.println("Result Bundle");
    	}
    };
    
    // wipe the callbacks
    @Override
    protected void onDestroy() {
        super.onDestroy();
        router.removeCallback(callback);
        router.getDefaultRoute().select();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // create the adapter for the routeList
        adapter = new ArrayAdapter<RouteLabelWrapper>(this, android.R.layout.select_dialog_item, android.R.id.text1);

        // start up the media router
        router = router.getInstance(this);

        // add all existing routes to the adapter
        for (MediaRouter.RouteInfo route: router.getRoutes()) {
            if (route.isDefault())
                continue;
            adapter.add(new RouteLabelWrapper(route));
            //route.
        }

        // scan for new routes
        selector = new MediaRouteSelector.Builder().addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK).build();
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);

        routeList = (ListView)findViewById(R.id.list);
        routeList.setAdapter(adapter);

        // on click, start playback of trailer
        routeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                curRoute = adapter.getItem(position).routeInfo;
                
                Runnable r=new Runnable()
                {
                    public void run() 
                    {
                    	try {
                            // select the route for usage
                            router.selectRoute(curRoute);
                         // old request no longer supported...
                            curRoute.sendControlRequest(new Intent(MediaControlIntent.ACTION_START_SESSION), mrCRC);
                            Intent pIntent = new Intent(MediaControlIntent.ACTION_PLAY); //MediaControlIntent.ACTION_PLAY, videoUri);
                            //Intent(MediaControlIntent.ACTION_PLAY);
                            pIntent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
                            pIntent.setDataAndType(videoUri, "video/mp4");
                            
                            // testing route to see if it supports Remote playback intent
                            if(curRoute.supportsControlRequest(pIntent)) {
                            	System.out.println("YES");
                            } else {
                            	System.out.println("NOPE");
                            	txtStatus.setText("Remote Playback not supported on selected Device.");
                            	return;
                            }
                            //curRoute.
                            ccSelected = true;
                            // send the play control request with the video uri
                            // also pause, resume - reverses pause, seek, and stop
                            //curRoute.sendControlRequest(new Intent(MediaControlIntent.ACTION_PLAY, videoUri), new MediaRouter.ControlRequestCallback() {
                            //});
                            //curRoute.sendControlRequest(pIntent, mrCRC);
                            mRemotePlaybackClient = new RemotePlaybackClient(getApplicationContext(), curRoute);
                            // Send file for playback
                            mRemotePlaybackClient.play(videoUri,
                                    "video/mp4", null, 0, null, new ItemActionCallback() {

                                    @Override
                                    public void onResult(Bundle data, String sessionId,
                                    		android.support.v7.media.MediaSessionStatus sessionStatus,
                                            String itemId, android.support.v7.media.MediaItemStatus itemStatus) {
                                        System.out.println("play: succeeded for item " + itemId);
                                        txtStatus.setText("play: succeeded for item " + itemId);
                                    }

                                    @Override
                                    public void onError(String error, int code, Bundle data) {
                                    	System.out.println("play: failed - error:"+ code +" - "+ error);
                                    	txtStatus.setText("play: failed - error:"+ code +" - "+ error);
                                    }
                                });


            }catch (Exception ex) {
            	ex.printStackTrace();
            }

                    }
                };
                runOnUiThread(r);
            }
        });
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        routeList.setEmptyView(findViewById(R.id.empty));
    }
    
    
    public void doMCI(MediaRouter.RouteInfo tRoute, String action, String tURI) {
    		// action = play, pause, resume, stop, seek...
    	if(action.equals(MediaControlIntent.ACTION_PLAY)) {
    		
    	} else if(action.equals(MediaControlIntent.ACTION_PAUSE)) {
    		
    	} else if(action.equals(MediaControlIntent.ACTION_RESUME)) {
    		
    	} else if(action.equals(MediaControlIntent.ACTION_SEEK)) {
    		
    	} else if(action.equals(MediaControlIntent.ACTION_STOP)) {
    		
    	} else if(action.equals(MediaControlIntent.ACTION_GET_STATUS)) {
    		
    	}
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
//        router.removeCallback(callback);

    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
  //  	router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }
    
    
}

