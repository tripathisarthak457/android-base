devRelease, arm64-v8a split, R8 on. Package columns are defined dex bytes (apkanalyzer, mapping applied).

| Build | APK (bytes) | dex, uncompressed (bytes) | dex files | `androidx.room` | `androidx.room3` | `androidx.sqlite` | `androidx.work` |
|---|---:|---:|---:|---:|---:|---:|---:|
| before-standard | 2,120,539 | 3,563,824 | 1 | 30,557 | 0 | 9,994 | 0 |
| after-standard | 2,098,646 | 3,512,688 | 1 | 0 | 65,465 | 5,618 | 0 |
| before-everything | 4,606,428 | 6,286,676 | 1 | 57,546 | 0 | 10,861 | 88,666 |
| after-everything | 4,024,923 | 6,174,748 | 1 | 42,635 | 79,011 | 15,297 | 99,027 |
| after-everything-noqueue | 3,901,683 | 5,939,404 | 1 | 0 | 77,748 | 5,639 | 0 |
