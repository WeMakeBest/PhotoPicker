Custom-built photo and video picker with custom UI.
--------

⚠️ **Warning**: This library has not yet been tested in all Android versions.

---
**Features**
--------

* Can be used to pick images and videos.
* Customizable theme.
* Max selections limit.
* Option to take photos or record videos directly.
* All permissions are handled by the library itself.
* Easy to use.

---

**Installation:**
--------

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```ts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' } //Add this line
    }
}
```

**Step 2.** Add the dependency

```ts
dependencies {
    implementation 'com.github.WeMakeBest:PhotoPicker:1.1.2'
}
```

---

**Basic Usage:**
--------

Declare result launcher in the activity where you want to use PhotoPicker

```ts
private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
```

Init result launcher in onCreate() of activity

```ts
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_sample)
    
    imagePickerLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.data != null) {
            val items : ArrayList<MediaItemModel> = result.data!!.getParcelableArrayListExtra(
            PhotoPicker.MEDIA)!!
            
            // TODO: Do Anything with the result :)
            Log.d("PickedMediaItems", items.toString())
        }
    }
}
```

Launch picker

```ts
btnLaunchPicker.setOnClickListener {
    imagePickerLauncher.launch(
        PhotoPicker.getIntent(
            this, //Context
            PhotoPicker.Type.BOTH, //Media type (IMAGES, VIDEOS, BOTH)
            10 //Max Selections
        )
    )
}
```

**Customize:**
--------

Override these color values and add yours to change the PhotoPicker theme

```ts
<!--    Override these colors to change theme of photo picker-->
    <color name="photo_picker_color_primary_light">#F4BDFD</color>
    <color name="photo_picker_color_primary">#9C27B0</color>
    <color name="photo_picker_color_primary_dark">#641971</color>
```
