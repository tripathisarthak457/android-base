# R8 rules for the catalog.
#
# The catalog's own code has no serialization and no reflection, and the libraries it uses ship
# their own rules, so there is nothing here beyond keeping crash reports readable.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
