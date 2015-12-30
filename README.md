tranSMARTClient
===============
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/transmart-android/localized.svg)](https://crowdin.com/project/transmart-android)

The tranSMART Android client is an REST API client for the tranSMART data warehouse (http://transmartfoundation.org), which enables you to browse subject-level study data from multiple tranSMART servers on your Android phone or tablet.  

Please note that you need to have the transmart-rest-api plugin installed on your server to be able to talk to it: https://github.com/transmart/transmart-rest-api. Allow the android-client to connect to it by adding the following definition to the clients:
```
[  
      clientId: 'android-client',  
      clientSecret: '',  
      authorities: ['ROLE_CLIENT'],  
      scopes: ['read'],  
      authorizedGrantTypes: ['authorization_code', 'refresh_token'],  
      redirectUris: ['transmart://oauthresponse']  
  ]
  ```

The application is available on Google Play, but currently in Alpha testing. Join the alpha testing group at http://bit.ly/testtransmart.

[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=nl.thehyve.transmartclient)

The tranSMARTClient is currently being developed in The Hyve. Find us at http://thehyve.nl.
