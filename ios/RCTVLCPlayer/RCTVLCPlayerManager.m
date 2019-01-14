#if __has_include(<React/RCTBridge.h>)
#import <React/RCTBridge.h>
#import <React/RCTUIManager.h>
#import <React/UIView+React.h>
#else
#import "RCTBridge.h"
#import "RCTUIManager.h"
#endif

#import "RCTVLCPlayerManager.h"
#import "RCTVLCPlayer.h"

@implementation RCTVLCPlayerManager

RCT_EXPORT_MODULE();

@synthesize bridge = _bridge;

- (UIView *)view
{
    return [[RCTVLCPlayer alloc] initWithEventDispatcher:self.bridge.eventDispatcher];
}

/* Should support: onLoadStart, onLoad, and onError to stay consistent with Image */
RCT_EXPORT_VIEW_PROPERTY(onVideoProgress, RCTBubblingEventBlock);
/*RCT_EXPORT_VIEW_PROPERTY(onVideoPaused, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoStopped, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoBuffering, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoPlaying, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoEnded, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoError, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoOpen, RCTBubblingEventBlock);*/
RCT_EXPORT_VIEW_PROPERTY(onVideoLoadStart, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onSnapshot, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onIsPlaying, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoStateChange, RCTBubblingEventBlock);


- (dispatch_queue_t)methodQueue
{
//    return dispatch_get_main_queue();
    return _bridge.uiManager.methodQueue;
}

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL);
RCT_EXPORT_VIEW_PROPERTY(seek, float);
RCT_EXPORT_VIEW_PROPERTY(rate, float);
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL);
RCT_EXPORT_VIEW_PROPERTY(volume, int);
RCT_EXPORT_VIEW_PROPERTY(volumeUp, int);
RCT_EXPORT_VIEW_PROPERTY(volumeDown, int);
RCT_EXPORT_VIEW_PROPERTY(resume, BOOL);
RCT_EXPORT_VIEW_PROPERTY(videoAspectRatio, NSString);
RCT_EXPORT_VIEW_PROPERTY(snapshotPath, NSString);

RCT_EXPORT_METHOD(takeSnapshot:(nonnull NSNumber *)viewId :(NSString*)path :(RCTPromiseResolveBlock)resolve
            :(RCTPromiseRejectBlock)reject)
{
    [self withView:viewId :^(RCTVLCPlayer* view){
        int value = [view takeSnapshot:path];
        if(value >= 0){
            resolve(@(value));
        }else{
            reject([NSString stringWithFormat:@"%d",value],@"no video out",nil);
        }
    } :resolve :reject];
}

RCT_EXPORT_METHOD(startRecord:(nonnull NSNumber *)viewId :(NSString*)fileDirectory :(RCTPromiseResolveBlock)resolve
            :(RCTPromiseRejectBlock)reject)
{
    [self withView:viewId :^(RCTVLCPlayer* view){
        BOOL value = [view startRecord:fileDirectory];
        resolve([NSNumber numberWithBool: value]);
    } :resolve :reject];
}

RCT_EXPORT_METHOD(stopRecord:(nonnull NSNumber *)viewId :(RCTPromiseResolveBlock)resolve
        :(RCTPromiseRejectBlock)reject)
{
    [self withView:viewId :^(RCTVLCPlayer* view){
        BOOL value = [view stopRecord];
        resolve([NSNumber numberWithBool: value]);
    } :resolve :reject];
}

- (void) withView:(nonnull NSNumber *)viewId :(void(^)(RCTVLCPlayer*))handler :(RCTPromiseResolveBlock)resolve
        :(RCTPromiseRejectBlock)reject {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, RCTVLCPlayer *> *viewRegistry) {
        // 找到目标View实例
        RCTVLCPlayer *view = viewRegistry[viewId];
        if (![view isKindOfClass:[RCTVLCPlayer class]]) {
            RCTLogError(@"Invalid view returned from registry, expecting TBNAnimationView, got: %@", view);
            reject(@"with_view",@"Unexpected RCTVLCPlayer type",nil);
        } else {
            // 调用View的方法
            handler(view);
        }
    }];
}

@end
