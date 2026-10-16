These two recordings did not drive the control. On the phone `uiautomator dump` left the previous dump in place
(cp1_record.py does not delete it before dumping and sends the dump's errors to /dev/null), so the script tapped
coordinates from an older screen: the sheet recording shows the catalog index scrolling, the toast one an untouched
page. They are kept only as evidence of that. The sheet and toast motion is in ../../cp1/ (emulator) and is covered
by SheetTest, OverlayHostTest and RecompositionJourneysTest.
