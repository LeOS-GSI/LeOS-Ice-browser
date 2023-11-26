sed -i \
    -e '/Deps.mozilla_lib_push_firebase/d' \
    -e '/Deps.adjust/d; /Deps.installreferrer/d; /Deps.google_ads_id/d' \
    -e '/Deps.google_play_store/d' \
    app/build.gradle

