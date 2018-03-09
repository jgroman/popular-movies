# Popular Movies

An educational Android app for learning Android programming concepts.

Udacity Nanodegree project, Stage 1

## Important note regarding TMDb API token  
Since it is illegal to publicly share personal TMDb API token, said token is stored externally
in personal gradle.properties file and included to app at build time.  

__How to store your own key:__
1. Navigate to your own *<USER_HOME_DIR>/.gradle* directory
2. Locate *gradle.properties* file. If it is not present, create new empty one.
3. Add following line to *gradle.properties* file: `TMDbApiToken="PUT_YOUR_TOKEN_HERE"`

Failing to do so, you will not be able to build the app.

## External libraries used:
Picasso - http://square.github.io/picasso/



