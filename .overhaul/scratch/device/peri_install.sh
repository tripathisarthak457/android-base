source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.jorwen.periflow.dev
$A install /home/crazyhusir/Documents/projects/jorwen/periflow/periflow-android/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
$A shell cmd package resolve-activity --brief $PKG | tail -1
