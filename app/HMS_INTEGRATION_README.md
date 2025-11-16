Huawei Push Kit (HMS) integration instructions
-----------------------------------------------

To enable Huawei Push Kit for EMUI devices as a fallback notification channel, follow these steps:

1) Create a project in AppGallery Connect and register your app. Download `agconnect-services.json` and place it into `app/` directory.
2) Add HMS dependencies to `app/build.gradle` (example):
   implementation 'com.huawei.hms:push:6.6.0.300'  // verify latest version
   implementation 'com.huawei.agconnect:agconnect-core:1.8.0.300' 

3) Add AGC plugin in top-level build.gradle and apply in app/build.gradle (see HMS docs).

4) Add required manifest entries, for example (snippet):
   <service android:name="com.huawei.hms.push.HmsMessageService" android:exported="true">
       <intent-filter>
           <action android:name="com.huawei.push.action.MESSAGE"/>
       </intent-filter>
   </service>

5) Implement token retrieval and server-side registration. Use `HuaweiPushHelper.registerPush(context)` placeholder to start registration.

Limitations and notes:
- You need AppGallery Connect credentials to obtain `agconnect-services.json`.
- For CI builds, include a placeholder `agconnect-services.json` or add conditional build steps to include it via secrets.
- HMS usage requires compliance with Huawei policies.
