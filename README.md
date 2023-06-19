# Nexus

A fork of the **Radenso Nexus** Android app at
[https://github.com/nolimits-enterprises/RadensoNexus](https://github.com/nolimits-enterprises/RadensoNexus).

[![Android CI](https://github.com/jsdx761/Test/actions/workflows/android.yml/badge.svg)](https://github.com/jsdx761/Test/actions/workflows/android.yml)

# Project motivation and history

I was looking for a simple Android companion app for my [Radenso DS1 radar detector](https://radenso.com/products/radenso-ds1)
with an integrated user experience and the following features:

* Radar detector alerts, crowd-sourced reports, and alerts about potential
  surveillance aircrafts all combined in a single prioritized list;
* Voice announcements with a soft relaxing voice matching the UK sounding
  voice of my Jaguar builtin car navigation system;
* Non-stressful sound notification follow-up reminders for alerts in range
  after the original voice announcements;
* Announcements of important device, network, and location status;
* Reliable audio stream ducking with my phone and car setup to be able to
  listen to music over Bluetooth and still get clear voice announcements
  of the alerts blended with the music with an acceptable volume and without
  delays or interruption of the music audio;

Basically a simple "hands off", "eyes on the road", "no stress" voice based
user experience.

I had tried various combinations of existing apps with mixed results:
* [Radenso Nexus](https://play.google.com/store/apps/details?id=com.noLimits.TheiaNexus) -
  missing voice, crowded-sourced reports, or aircrafts;
* [JBV1](https://play.google.com/store/apps/details?id=com.johnboysoftware.jbv1) -
  missing DS1 support, voice and audio conflicts with other apps, annoying
  notifications when losing cell connection;
* [Highway Radar](https://play.google.com/store/apps/details?id=com.highwayradar.app) -
  issues with audio stream ducking and blending;
* [Waze](https://play.google.com/store/apps/details?id=com.waze) - short
  distances for crowd-sourced reports, and requiring a navigation route to get
  those reports.

Then I found Radenso's open-source code of the **Radenso Nexus** app on Github
here:
[https://github.com/nolimits-enterprises/RadensoNexus](https://github.com/nolimits-enterprises/RadensoNexus)

The code looked reasonable to me so I forked it, removed what I didn't need to
simplify things and added the voice and alerting features I wanted.

I renamed the app **Nexus** from **Radenso Nexus** to make it clear that it's
a fork and not the original app from Radenso. Great thanks to Radenso for a
nice job with the original codebase by the way!

**Nexus** is barebones compared to the more sophisticated apps out there like
JBV1 or Highway Radar, but does just what I need and I'm hoping others will
find it useful as well.

The original code from Radenso has been ported to a recent Android SDK and
Android 13, refactored and commented to make it easier to work with. The
implementation of the voice announcements, crowd-sourced reports, and
detection of surveillance aircrafts are just a little bit of code on top of
the original codebase.

Most of the preferences (timers for fetching reports, distance thresholds,
speech rate and pitch, various internal settings, test data etc) are defined
as constants in Configuration.java and should be easy to adjust if needed.

Please feel free to build and try the **Nexus** app yourself (see below for
the steps to do that), contribute code fixes or improvements, or just fork
again and adapt to your own preferences.

Hope that helps...

# Building the app yourself

This section assumes that you're familiar with Android app development using
Java, the Android SDK, and Gradle on Linux or MacOS. The steps described here
are known to work with the following environment and requirements:
```
Android SDK Platform Tools v34
Android SDK v34
Java Open JDK v17
```

### Downloading aircraft databases

**Nexus** can optionally recognize potential surveillance aircrafts. If you
don't need that capability, skip this section and move on to the
**Assembling the app** section.

To collect up-to-date public aircraft information databases and include a
list of potential surveillance aircrafts in the app, do the following:

Download the latest aircraft database from Opensky listed here:

[https://opensky-network.org/datasets/metadata/](https://opensky-network.org/datasets/metadata/)

For example:

[https://opensky-network.org/datasets/metadata/aircraft-database-complete-2023-06.csv](https://opensky-network.org/datasets/metadata/aircraft-database-complete-2023-06.csv)

Save the file as:
```
scripts/data/opensky_aircrafts.csv
```

Download the latest aircraft database from the FAA listed here:

[https://www.faa.gov/licenses_certificates/aircraft_certification/aircraft_registry/releasable_aircraft_download](https://www.faa.gov/licenses_certificates/aircraft_certification/aircraft_registry/releasable_aircraft_download)

Direct link:

[https://registry.faa.gov/database/ReleasableAircraft.zip](https://registry.faa.gov/database/ReleasableAircraft.zip)

Save the file as:
```
scripts/data/faa_aircrafts.zip
```

To include a list of aircrafts in the app, run the following commands:
```
cd scripts
python ./filter-faa-aircrafts.py
python ./filter-opensky-aircrafts.py
python ./merge-interesting-aircrafts.py
cp ./data/interesting_aircrafts.csv ../app/src/main/assets/interesting_aircrafts.csv
```

### Assembling the app

To assemble the **Nexus** app, run the following commands:
```
./gradlew build
```

You should see the following:
```
BUILD SUCCESSFUL
```

# Installing the app using ADB

To connect your Android phone to your computer, do the following:

On the phone, turn "Settings / Developer options / Wireless debugging" **on**,
then select "Pair device with pairing code".

On the computer run the following commands:
```
adb pair <pairing IP address and port> <pairing code>
adb connect <wireless debugging IP address and port>
adb devices
```

You should see your Android phone in the list.
```
List of devices attached
<IP address and port> device
```

From the Nexus directory run the following commands:
```
adb install -r ./app/build/outputs/apk/debug/app-debug.apk
```

You should see the following:
```
Performing Streamed Install
Success
```

**Nexus** should now be installed on your phone. It does not replace the
original **Radenso Nexus** app as it has a different name so you can keep
both apps on the phone if needed.

### Using Android Studio

If you prefer to use the Android Studio IDE to build and install the app, open
the Nexus directory as a project in Android Studio and go from there with the
IDE.

# Downloading a build of the app from Github

The Github project is configured with an Android CI Github action workflow
that builds the app there on Github for you.

The app's APK is available for download in the artifacts section of the
Android CI workflow runs here:
[https://github.com/jsdx761/Nexus/actions/workflows/android.yml](https://github.com/jsdx761/Nexus/actions/workflows/android.yml)

Once you've downloaded the APK, install it using ADB like described above, or
using any of the usual techniques for installing APKs on an Android device.

The builds on Github do not include the aircraft databases, you will need
to build the app's APK yourself as described above if you want that feature.

# Using the Nexus app

**Nexus** is a variation of the original **Radenso Nexus** app. It primarily
adds voice announcements, crowd-sourced reports, and alerts about potential
surveillance aircrafts on top of the original DS1 radar detector alerts.

Note that the original DS1 radar detector settings screens have been removed
to help simplify the app. Use the original Radenso app when you need those.

The first time you launch **Nexus** it'll request various permissions to
access Bluetooth (to connect to your DS1 radar detector) and your location
(to find crowd-sourced reports and aircrafts close to your location).

The main menu provides the following options:

* **Threats** - Select this to see a combined list of threats from your DS1
  radar detector, crowd-sourced reports, and surveillance aircrafts; you will
  need to configure the crowd-sourced reports and aircrafts under **Sources**
  below to see those threats; you'll also get voice announcements on threats
  and important events as they come up followed by regular sound reminders.

* **Sources**

  * **DS1 Radar** - Select this to scan for your DS1 radar detector; select
    it once it shows in the list and the app will automatically connect to it
    later on.

  * **Reports** - Select this to configure the URL of a crowd-sourced server
    similar to Waze for example; note that entering https://www.waze.com may
    work as Nexus uses a protocol similar to the Waze protocol, but make sure
    that doesn't violate the Waze terms of service in your country;

  * **Aircrafts** - Select this to configure the URL of an aircraft report
    server; if you are using the default aircraft info server
    [https://opensky-network.org/](https://opensky-network.org/) enter
    your OpenSky user info to get reports every few seconds instead of minutes.

* **DS1 Volume** - Select this to control the volume of your DS1 radar detector
  directly from the app.

Voice announcements and sound notifications play on the phone's **Voice call**
audio stream. You will need to select your phone's Bluetooth as audio source
in the vehicle, then if you stream music the app will duck  music as
needed so you can clearly hear the announcements. Most Android phones let you
set separate volumes for the music **Media** and **Voice call** streams, so
you should be able to control how loud you want the voice announcements to be
over your music.

# Sample screens and audio

TODO add screenshots and maybe recordings of the audio experience here

# Limitations

TODO describe some of the limitations of the report selection and aircraft
recognition detection algorithms, and how the app just relies on the DS1 radar
detector for filtering etc and doesn't do all the additional smarts that you
get with the other apps out there.

# Disclaimers

This is a personal weekend project on my personal time not related in any way
with whatever software development work I may do for whoever I work for on my
day job.

Do not use the app to violate the terms of service of any crowd-sourced report
server you decide to use (e.g. Waze).

# Resources

A list of public resources that helped build the app:

[Radenso Github repositories](https://github.com/nolimits-enterprises)

[Vortex's how to setup your Radenso DS1](https://www.vortexradar.com/2021/08/how-to-set-up-configure-your-radenso-ds1/)

[Vortex's FAQ](https://www.vortexradar.com/faq/)

[RD Forum](https://www.rdforum.org/)

[The OpenSky API Network REST API](https://openskynetwork.github.io/opensky-api/rest.html)

[ICAO Aviation Standards](https://www.icao.int/Pages/default.aspx)

[FAA Aircraft Registration Information](https://www.faa.gov/licenses_certificates/aircraft_certification/aircraft_registry/releasable_aircraft_download)

[The ADSB-Exchange](https://adsbexchange.com/)

[Waze for Cities](https://support.google.com/waze/partners/answer/13458165)

[Geospatial formulas for distance, bearing etc](https://www.movable-type.co.uk/scripts/latlong.html)

# A random list of ideas

A few more ideas:

* Use an ADS-B radio receiver to get aircraft information instead of relying
on an Internet connection;

* Alert when recognizing interesting P25 radio communication in range.

