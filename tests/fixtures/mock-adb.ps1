# Offline ADB fixture. No device access or external commands.
$call = $args -join ' '
if ($env:DRIVELITE_TEST_ADB_LOG) { Add-Content -LiteralPath $env:DRIVELITE_TEST_ADB_LOG -Value $call }
if ($call -match '^connect fixture-device$') { 'connected to fixture-device'; exit 0 }
if ($call -eq '-s fixture-device get-state') {
    if ($env:DRIVELITE_TEST_ADB_SCENARIO -eq 'unauthorized') { 'unauthorized'; exit 1 }
    'device'; exit 0
}
if ($call -notmatch '^-s fixture-device shell (.+)$') { 'Unsupported fixture command'; exit 80 }
$command = $Matches[1]
if ($command -match '^pm list packages(?: (?:-u|-d|-e|-s|--user|0))*$') {
    if ($env:DRIVELITE_TEST_ADB_SCENARIO -eq 'required-failure' -and $command -eq 'pm list packages --user 0') { 'Error: package query unavailable'; exit 0 }
    $names = @('android', 'com.android.systemui', 'com.tw.radio', 'com.google.android.backuptransport', 'com.google.android.gms', 'com.example.music')
    if ($command -match '(?:^| )-u(?: |$)') { $names += 'com.android.egg' }
    if ($command -match '(?:^| )-d(?: |$)') { $names = @('com.google.android.gms') }
    if ($command -match '(?:^| )-e(?: |$)') { $names = @($names | Where-Object { $_ -ne 'com.google.android.gms' }) }
    if ($command -match '(?:^| )-s(?: |$)') { $names = @($names | Where-Object { $_ -ne 'com.example.music' }) }
    $names | ForEach-Object { 'package:' + $_ }; exit 0
}
switch -Regex ($command) {
    '^getprop ro.product.model$' { 'Fixture TS7'; exit 0 }
    '^getprop ro.build.version.release$' { '8.1.0'; exit 0 }
    '^getprop ro.build.version.sdk$' { '27'; exit 0 }
    '^getprop ro.product.cpu.abi$' { 'armeabi-v7a'; exit 0 }
    '^am get-current-user$' { '0'; exit 0 }
    '^pm list users$' { 'Users: UserInfo{0:Owner:13} running'; exit 0 }
    '^wm size$' { 'Physical size: 1024x600'; exit 0 }
    '^cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME$' { 'com.dofun.variety/.Launcher'; exit 0 }
    '^dumpsys webviewupdate$' {
        if ($env:DRIVELITE_TEST_ADB_SCENARIO -eq 'optional-failure') { 'Unknown command: webviewupdate'; exit 1 }
        'Current WebView package: com.android.chrome'; exit 0
    }
    '^dumpsys meminfo$' { 'Total RAM: 1,049,056K'; 'Used RAM: 555,675K'; exit 0 }
    '^ps$' { 'USER PID PPID VSIZE RSS WCHAN PC NAME'; 'u0_a1 123 1 2000 1000 ffffffff 00000000 com.tw.radio'; exit 0 }
    '^dumpsys package$' { 'Packages:'; '  Package [com.tw.radio] (fixture):'; '    User 0: installed=true enabled=0'; exit 0 }
    '^settings get global (window_animation_scale|transition_animation_scale|animator_duration_scale)$' { 'null'; exit 0 }
    default { 'Rejected non-read-only or unknown fixture command: ' + $command; exit 81 }
}
