# Todone

Go from "to-do" to "done" without breaking [the chain](http://lifehacker.com/281626/jerry-seinfelds-productivity-secret).

## Setup

Todone is designed to be run as your own App Engine application. This section will walk you through deploying Todone for the first time.

1. Download the Google App Engine SDK for Go from the [downloads](https://cloud.google.com/appengine/downloads#Google_App_Engine_SDK_for_Go) page and add it to your `PATH`
2. Register a new App Engine application
3. Install [ok](https://github.com/willyg302/ok)
4. Deploy Todone!

```bash
ok init gh:willyg302/Todone
cd Todone
ok run build gae_deploy
```

During the `build` step, you will be prompted for an **App name**. **App name** is the name of your newly registered application.
