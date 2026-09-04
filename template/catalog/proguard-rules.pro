# R8 rules for the catalog.
#
# The catalog depends on :core:designsystem alone, which has no serialization and no reflection,
# so there is nothing here beyond keeping crash reports readable.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
