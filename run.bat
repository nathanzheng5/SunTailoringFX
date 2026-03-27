@echo off
java --module-path "C:\Program Files\Java\openjfx-19.0.2.1_windows-x64_bin-sdk\javafx-sdk-19.0.2.1\lib" ^
     --add-modules javafx.controls,javafx.fxml,javafx.web ^
     -jar "%~dp0SunTailoringFX.jar"