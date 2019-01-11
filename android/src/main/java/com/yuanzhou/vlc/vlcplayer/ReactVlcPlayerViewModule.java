package com.yuanzhou.vlc.vlcplayer;

import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

public class ReactVlcPlayerViewModule extends ReactContextBaseJavaModule {
    public static final String TAG = ReactVlcPlayerViewModule.class.getSimpleName();

    public ReactVlcPlayerViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ReactVlcPlayerViewModule";
    }

    @ReactMethod
    public void takeSnapshot(final int viewId, final String path, final Promise promise) {
        withView(viewId, promise, new ReactVlcPlayerViewViewHandler() {
            @Override
            public void handle(ReactVlcPlayerView view) {
                int value = view.takeSnapshot(path);
                if(value >= 0){
                    promise.resolve(value);
                }else{
                    promise.reject(String.valueOf(value),"no video out");
                }
            }
        });
    }

    @ReactMethod
    public void startRecord(final int viewId, final String fileDirectory, final Promise promise){
        withView(viewId, promise, new ReactVlcPlayerViewViewHandler() {
            @Override
            public void handle(ReactVlcPlayerView view) {
                boolean value = view.startRecord(fileDirectory);
                promise.resolve(value);
            }
        });
    }

    @ReactMethod
    public void stopRecord(final int viewId, final Promise promise){
        withView(viewId, promise, new ReactVlcPlayerViewViewHandler() {
            @Override
            public void handle(ReactVlcPlayerView view) {
                boolean value = view.stopRecord();
                promise.resolve(value);
            }
        });
    }

    private void withView(final int viewId, final Promise promise, final ReactVlcPlayerViewViewHandler handler) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof ReactVlcPlayerView) {
                    ReactVlcPlayerView myView = (ReactVlcPlayerView) view;
                    handler.handle(myView);
                }
                else {
                    Log.e(TAG, "Expected view to be instance of ReactVlcPlayerView, but found: " + view);
                    promise.reject("with_view", "Unexpected ReactVlcPlayerView type");
                }
            }
        });
    }

    interface ReactVlcPlayerViewViewHandler {
        void handle(ReactVlcPlayerView view);
    }
}
