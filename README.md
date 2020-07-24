# Godot Android Facebook Plugin
This is Android Facebook plugin for Godot 3.2.2 or higher.

# How to use
* On your Godot project install android build template. You can follow the [official documentation](https://docs.godotengine.org/en/latest/getting_started/workflow/export/android_custom_build.html)
* Go to Releases (on the right of this repository page) and download a released version. It is a ZIP file containing 2 files: "aar" of the plugin and "gdap" file describing it,
* Extract the contents of the released ZIP file to res://android/plugins directory of your Godot project
* On Godot platform choose: Project -> Export -> Options and make sure turn on the "Use Custom Build" and "Godot Facebook" on the "Plugins" section:
![Annotation 2020-07-24 121927](https://user-images.githubusercontent.com/3739222/88377830-8c48c300-cda8-11ea-8cf1-638bb1c230ee.png)

## Api Reference

**Functions:**
```
init(app_id)
appInvite(app_link_url, preview_image_url)
setFacebookCallbackId(get_instance_ID())
getFacebookCallbackId()
login()
logout()
isLoggedIn()  
```

**Callback functions:**
```
login_success(token)
login_cancelled()
login_failed(error)
```
