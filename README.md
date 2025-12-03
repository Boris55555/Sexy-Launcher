When you install this launcher app on your Mudita Kompakt phone, if you want to keep this as the default launcher app so that the default launcher of Mudita Kompakt doesn't appear, you need to run a couple of commands in the terminal and for that you need to have USB debugging turned on in the developer options.

**NOTE!** Do not copy and execute the **'bash'** tag that appears at the beginning of the command. It's just for syntax highlighting. Just copy the command below that starts with adb.

Once you have set Sexy Launcher as your default launcher, run in the terminal:
```bash adb shell pm disable-user --user 0 com.mudita.launcher ```

If you want to restore the default Mudita Compact launcher, run in the terminal:
```bash adb shell pm enable --user 0 com.mudita.launcher ```

And when you want to remove Sexy Launcher, run in the terminal (newer versions 1.14...):
```bash adb uninstall com.boris55555.sexylauncher ```

Older versions:
```bash adb uninstall com.example.sexylauncher ```

Pictures:

<img src="assets/AllApps.jpg" alt="Picture1" width="50" />
<img src="assets/Homescreen001.jpg" alt="Picture2" width="50" />
<img src="assets/Homescreen002.jpg" alt="Picture3" width="50" />
<img src="assets/Notifications.jpg" alt="Picture4" width="50" />
<img src="assets/Settings.jpg" alt="Picture5" width="50" />
