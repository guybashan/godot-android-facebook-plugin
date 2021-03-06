# Godot Android Facebook Plugin
This is Android Facebook plugin for Godot 3.2.2 or higher.

## How to use
* On your Godot project install android build template. You can follow the [official documentation](https://docs.godotengine.org/en/latest/getting_started/workflow/export/android_custom_build.html)
* Go to Releases (on the right of this repository page) and download a released version. It is a ZIP file containing 2 files: "aar" of the plugin and "gdap" file describing it,
* Extract the contents of the released ZIP file to res://android/plugins directory of your Godot project
* Create "res/values" folder to your Godot project under the folder: res://android/build.
* In the "res://android/build/res/values" folder create a new file named: strings.xml and make sure to properly set your facebook app id:
    ```
        <?xml version='1.0' encoding='UTF-8'?>
        <resources>
          <string name="facebook_app_id">12345..</string>
          <string name="fb_login_protocol_scheme">fb12345..</string>
        </resources>
    ```
* On Godot platform choose: Project -> Export -> Options and make sure turn on the "Use Custom Build" and "Godot Facebook" on the "Plugins" section:
![Annotation 2020-07-24 121927](https://user-images.githubusercontent.com/3739222/88377830-8c48c300-cda8-11ea-8cf1-638bb1c230ee.png)

## Basic Example in Godot (GDScript)
```
    const fb_app_id = "12345.."
    var fb
    if (Engine.has_singleton("GodotFacebook")):
        print("Facebook was detected")
        fb = Engine.get_singleton("GodotFacebook")
        fb.init(fb_app_id)
        fb.setFacebookCallbackId(get_instance_id())
        
        var permission = ["public_profile","email"]
        fb.login(permission)
    else:
        print("Facebook was not detected")
```

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
