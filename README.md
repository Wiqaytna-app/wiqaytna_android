# Wiqaytna Android

![alt text](./app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
 "Wiqaytna Logo")

Wiqaytna is the official Moroccan exposure notification app.

---

### Configs in gradle.properties

Sample Configuration
```
ORG="MAR"
STORE_URL="<Play store URL>"
PRIVACY_URL="<Privacy policy URL>"

SERVICE_FOREGROUND_NOTIFICATION_ID=771579
SERVICE_FOREGROUND_CHANNEL_ID="Wiqaytna Updates"
SERVICE_FOREGROUND_CHANNEL_NAME="Wiqaytna Foreground Service"

PUSH_NOTIFICATION_ID=771578
PUSH_NOTIFICATION_CHANNEL_NAME="Wiqaytna Notifications"

#service configurations
SCAN_DURATION=8000
MIN_SCAN_INTERVAL=36000
MAX_SCAN_INTERVAL=43000

ADVERTISING_DURATION=180000
ADVERTISING_INTERVAL=5000

PURGE_INTERVAL=86400000
PURGE_TTL=1814400000
MAX_QUEUE_TIME=7000
BM_CHECK_INTERVAL=540000
HEALTH_CHECK_INTERVAL=900000
CONNECTION_TIMEOUT=6000
BLACKLIST_DURATION=100000

FIREBASE_REGION = "<Your Firebase region>"

STAGING_FIREBASE_UPLOAD_BUCKET = "wiqayetna-app-staging"
STAGING_SERVICE_UUID = "17E033D3-490E-4BC9-9FE8-2F567643F4D3"

V2_CHARACTERISTIC_ID = "117BDD58-57CE-4E7A-8E87-7CCCDDA2A804"

PRODUCTION_FIREBASE_UPLOAD_BUCKET = "wiqaytna-app"
PRODUCTION_SERVICE_UUID = "B82AB3FC-1595-4F6A-80F0-FE094CC218F9"

android.useAndroidX=true
android.enableJetifier=true
```

> ORG: For international federation usage

> To obtain the official BlueTrace Service ID and Characteristic ID, please email [info@bluetrace.io](mailto:info@bluetrace.io)

---

### Build Configurations in build.gradle
Change the package name and other configurations accordingly such as the `resValue` in  in the different settings in `buildTypes`
For example,
```groovy
buildTypes {
    debug {
            buildConfigField "String", "FIREBASE_UPLOAD_BUCKET", STAGING_FIREBASE_UPLOAD_BUCKET
            buildConfigField "String", "BLE_SSID", STAGING_SERVICE_UUID

            String ssid = STAGING_SERVICE_UUID
            versionNameSuffix "-debug-${getGitHash()}-${ssid.substring(ssid.length() - 5,ssid.length() - 1 )}"
            resValue "string", "app_name", "Wiqaytna"
            applicationIdSuffix "stg"
        }
```

> Values such as STAGING_FIREBASE_UPLOAD_BUCKET, STAGING_SERVICE_UUID have been defined in gradle.properties as described above.

---

### Firebase and google-services.json
Setup Firebase for the different environment.
Download the google-services.json for each of the environments and put it in the corresponding folder.

Debug: ./app/src/debug/google-services.json

Production: ./app/src/release/google-services.json

The app currently relies on Firebase Functions to work. More information can be obtained by referring to [opentrace-cloud-functions](https://github.com/opentrace-community/opentrace-cloud-functions).

---

### Remote Config
Remote config is used for retrieving the "Share" message used in the app.
The key for it is "ShareText". If it is unable to be retrieved, it falls back to R.string.share_message

---

### Protocol Version
Protocol version used should be 2 (or above)
Version 1 of the protocol has been deprecated

---

### Security Enhancements
SSL pinning is not included as part of the repo.
It is recommended to add in a check for SSL certificate returned by the backend.

---

### Statement from Google
The following is a statement from Google:
"At Google Play we take our responsibility to provide accurate and relevant information for our users very seriously. For that reason, we are currently only approving apps that reference COVID-19 or related terms in their store listing if the app is published, commissioned, or authorized by an official government entity or public health organization, and the app does not contain any monetization mechanisms such as ads, in-app products, or in-app donations. This includes references in places such as the app title, description, release notes, or screenshots.
For more information visit [https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html](https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html)"

---

### Acknowledgements
Wiqaytna uses the following [third party libraries / tools](./ATTRIBUTION.md).
