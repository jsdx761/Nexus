# Nexus

A fork of the **Radenso Nexus** Android app at
[https://github.com/nolimits-enterprises/RadensoNexus](https://github.com/nolimits-enterprises/RadensoNexus).

[![Android CI](https://github.com/jsdx761/Nexus/actions/workflows/android.yml/badge.svg)](https://github.com/jsdx761/Nexus/actions/workflows/android.yml)

# Project motivation and history

I was looking for a simple Android companion app for my [Radenso DS1 radar detector](https://radenso.com/products/radenso-ds1)
with an integrated user experience and the following features:

* Radar detector alerts, crowd-sourced alerts, and alerts about potential
  surveillance aircrafts all combined in a single prioritized list;
* Voice announcements with a soft relaxing voice matching the UK sounding
  voice of my Jaguar builtin car navigation system;
* Non-stressful sound notification reminders for alerts in range after the
  original voice announcements;
* Announcements of important device, network, and location status;
* Reliable audio stream ducking with my phone and car setup to be able to
  listen to music over Bluetooth and still get clear voice announcements
  of the alerts blended with the music with an acceptable volume and without
  delays or interruption of the music audio;

Basically a simple "hands off", "eyes on the road", "no stress" voice based
user experience.

I had tried various combinations of existing apps with mixed results:
* [Radenso Nexus](https://play.google.com/store/apps/details?id=com.noLimits.TheiaNexus) -
  missing voice, crowded-sourced alerts, or aircraft alerts;
* [JBV1](https://play.google.com/store/apps/details?id=com.johnboysoftware.jbv1) -
  missing DS1 support, had voice and audio conflicts with other apps, and
  annoying notifications when losing cell connection;
* [Highway Radar](https://play.google.com/store/apps/details?id=com.highwayradar.app) -
  issues with audio stream ducking and blending;
* [Waze](https://play.google.com/store/apps/details?id=com.waze) - short
  distances for crowd-sourced alerts, and requiring a navigation route to get
  voice announcements of the alerts.

Then I found Radenso's open-source code of the **Radenso Nexus** app on Github
here:
[https://github.com/nolimits-enterprises/RadensoNexus](https://github.com/nolimits-enterprises/RadensoNexus)

The code looked reasonable to me so I forked it, removed what I didn't need to
simplify things and added the voice and alerting features I wanted.

I renamed the app **Nexus** from **Radenso Nexus** to make it clear that it's
a fork with siginificant changes and not the original app from Radenso. Many
thanks to Radenso for a nice job with the original codebase by the way!

**Nexus** is barebones compared to the more sophisticated apps out there like
JBV1 or Highway Radar, but now does just what I needed and I'm hoping others
will find it useful as well.

The original code from Radenso has been ported to a recent Android SDK and
Android 13, refactored and commented to make it easier to work with. The
implementation of the voice announcements, crowd-sourced alerts, and
detection of surveillance aircrafts are just a little bit of code on top of
the original codebase.

Most of the preferences (timers for fetching alerts, distance thresholds,
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
Android 13 on a Pixel 7
Android SDK Platform Tools v34
Android SDK v34
Java Open JDK v17
```

### Downloading aircraft databases

**Nexus** can optionally detect potential surveillance aircrafts. If you
don't need that capability, you can skip this section and move on to the
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

After the build completes, you should see the following:
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

# Using Android Studio

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

# Using the Nexus app

**Nexus** is a variation of the original **Radenso Nexus** app. It primarily
adds voice announcements, crowd-sourced alerts, and alerts about potential
surveillance aircrafts on top of the original DS1 radar detector alerts.

Note that the original DS1 radar detector settings screens have been removed
to help simplify the app. Use the original app from Radenso when you need
those.

The first time you launch **Nexus** it'll request various permissions to
access Bluetooth (to find and connect to your DS1 radar detector) and your
location (to look for crowd-sourced alerts and aircrafts close to your
location).

The main screen of the app will show a combined list of **Alerts** from your
DS1 radar detector, about crowd-sourced alerts, and potential surveillance
aircrafts. You'll also get voice announcements of those alerts and important
status events as they come up, followed by regular sound reminders.

Before you can see any alerts on the main screen, you will need to go to the
**Settings** screen to select which types of alerts you're interested in, and
configure the connection to your DS1 radar detector, and the URLs of the
crowd-sourced alerts and aircrafts tracking servers you wish to use.

The **Settings** menu provides the following options:

* **Alert Sources**

  * **Radar Detector** - Select this to scan for your DS1 radar detector;
    select it once it shows in the list and the app will automatically connect
    to it later on.

    * **Volume** - Select this to control the volume of your DS1 radar detector
      directly from the app. I typically set it to 0 if I'm going to be using
      the voice and sound alerts from the app.

  * **Crowd-sourced Reports** - Select this to configure the URL of a
    crowd-sourced server similar to Waze for example; note that entering
    [https://www.waze.com](https://www.waze.com) may work as Nexus uses a
    protocol compatible with the Waze protocol, but make sure that using the
    Waze server in that fashion doesn't violate the Waze terms of service in
    your country;

  * **Aircraft Alerts** - Select this to configure the URL of an aircraft
    tracking server; if you are using the default server
    [https://opensky-network.org](https://opensky-network.org), entering
    your OpenSky user info will give you a better API call rate limit
    allowing the app to get alerts every few seconds instead of every few
    minutes.

Voice announcements and sound notifications will play on the phone's
**Music** audio stream. You will need to select your phone's Bluetooth as
audio source in the vehicle, then if you stream music for example the app
will duck the music as needed so you can clearly hear the announcements.

# Sample screens and audio

TODO add screenshots and maybe recordings of the audio experience here

# Implementation choices and limitations

Nexus completely relies on the DS1 radar detector for alert filtering,
lockouts, muting etc without any smarts on top. Other apps like JBV1 and
Highway Radar have much more sophisticated logic but for me the DS1 was enough
and just more predictable. So the app reports exactly what it gets from the
DS1 unit, no less, no more. The Radenso folks know what they're doing with
their radar detector, and I wasn't going to pretend to do better.

The handling of crowd-sourced and aircraft alerts is a bit simplistic but I
was looking for a simple algorithm that I could just trust as I didn't feel
like trusting the fancy scoring logic in some of the other apps which for
example would favor alerts in front vs side or back or take into account the
number of thumbs up on those crowd-sourced alerts. Nexus simply reports the
closest crowd-sourced alerts within a 2-mile radius, and aircraft alerts
within a 5-mile radius.

I didn't feel like restricting the reporting to crowd-sourced alerts on the
current road or navigation route as I saw instances of alerts incorrectly
reported on side roads and I also wanted to get alerts on threats potentially
approaching from side roads as well, so a more brute-force just reporting
alerts within a certain radius felt a bit more safe after all.

Aircraft alerts don't implement any fancy flight pattern recognition like
some of the other apps do as that looked to me a bit far-fetched and the
operation/ownership info on aircrafts in OpenSky and FAA aircraft databases
already gave a good enough indication of which aircrafts could be potential
surveillance aircrafts. Again here I favored reporting more aircrafts
than less just to be safe, and there's not that many that it gives too many
false positives anyway.

Alert announcements simply indicate the distance and the bearing in 12-oclock 
format, for example: "Speed trap at 11 o-clock 1.5 mile away on I-280", or
"California Highway Patrol Eurocopter Helicopter at 7 o-clock 2 miles away".

Reminders are announced each time crowd-sourced alerts get 1/4 mile closer or
aircraft alerts get 1 mile closer, or when the bearing to the alert changes by
at least 3 hours.

A short sound notification reminder is played regularly while any crowd-sourced
or aircrafts alerts are active and in range, so you know you can't relax yet.

Once all alerts are gone or out of range the app plays an "Alerts are all
clear now" announcement, so you know you can finally relax.

Radar detector on/off, internet connectivity on/off, location on/off, and
availability of crowd-sourced and aircraft alerts are announced as well, but
intentionally after a short delay to minimize unnecessary distractions as many
of those conditions typically resolve by themselves after a few seconds.

I initially wanted to use to Bluetooth Synchronous Connection Oriented / Car
Audio Interrupt to have the app interrupt all audio sources on the car to play
alert announcements, but that introduced a delay of about 1-second for each
announcement, so I eventually favored using the Android Music stream instead
to get audio alerts right away.  That means that you'll need to select your
phone's Bluetooth as Audio input on the car. That works well for me as I
usually stream music from the phone (or sometimes just keep the phone silent
but still selected as Audio input) but if you prefer to use Bluetooth SCO / CAI
to have the ability to interrupt other audio sources, it should be fairly easy
to implement with a few minor changes to the app.

Again, the complete source of the app is open-sourced here on Github so feel
free to to tweak whatever you need to make it work for you! :-)

# Disclaimers

This is a personal weekend project on my personal time not related in any way
with whatever I may do for whoever I may work for on my day job.

Do not use the app to violate the terms of service of any crowd-sourced alert
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

# A few more ideas

A few more ideas of potential future additions to the app:

* Use a Software Defined Radio dongle, a Raspberry Pi, and tools like
**dump1090** and **dump978** to detect aircrafts instead of having to rely on
a working Internet connection to a server like OpenSky;

* Alert when detecting radio communication on certain public safety
frequencies, in particular 138Mhz-174Mhz, 380Mhz-512Mhz, 769Mhz-824Mhz and
851Mhz-869Mhz using a fast power spectrum tool like **rtl-power-fftw**; the
app wouldn't record or attempt to decrypt the communication, it'd just alert
on detecting radio power patterns over a baseline.

All the radio detection work would happen on the Raspberry Pi, and the Android
app would receive alerts from it using a simple data protocol over Bluetooth
Low Energy similar to how it communicates with the DS1 radar detector.

Links to working forks of those tools:

[dump1090](https://github.com/jsdx761/dump1090)

[dump978](https://github.com/jsdx761/dump978)

[rtl-power-fftw](https://github.com/jsdx761/rtl-power-fftw)

