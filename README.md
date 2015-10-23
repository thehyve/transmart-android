tranSMARTClient
===============

tranSMARTClient is an Android Client for TranSMART, which will enable you to browse the studies and its data on a tranSMART server.

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
