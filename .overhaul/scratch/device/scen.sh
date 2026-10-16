# Usage: scen.sh <pkg> <tag> <scenario...>   scenarios: tabs pushpop pushpop_gesture scroll_home scroll_feed tab1
source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=$1; TAG=$2; shift 2
TABS_X=${TABS_X:-"284 412 539 666 793 923 156"}; TAB_Y=${TAB_Y:-2213}
HOME_X=${HOME_X:-156}; FEED_X=${FEED_X:-284}
ITEM=${ITEM:-"540 782"}
run() { $A shell "$1"; }
reset() { $A shell dumpsys gfxinfo $PKG reset >/dev/null; }
save() { $A shell dumpsys gfxinfo $PKG > $R/${TAG}_$1.txt; $A shell dumpsys gfxinfo $PKG framestats > $R/${TAG}_$1_fs.txt; }
for S in "$@"; do
  case $S in
    tabs)
      run "input tap $HOME_X $TAB_Y"; sleep 1.5; reset
      run "for c in 1 2 3; do for x in $TABS_X; do input tap \$x $TAB_Y; sleep 0.9; done; done"
      sleep 1; save tabs;;
    tab1)  # one switch Home->Feed, then Feed->Home, framestats detail each
      run "input tap $HOME_X $TAB_Y"; sleep 2; reset; run "input tap $FEED_X $TAB_Y"; sleep 1.5; save tab1_home2feed
      reset; run "input tap $HOME_X $TAB_Y"; sleep 1.5; save tab1_feed2home;;
    pushpop)
      run "input tap $HOME_X $TAB_Y"; sleep 1.5; reset
      run "for i in 1 2 3 4 5 6 7 8; do input tap $ITEM; sleep 1.3; input keyevent 4; sleep 1.3; done"
      sleep 1; save pushpop;;
    push1)
      run "input tap $HOME_X $TAB_Y"; sleep 1.5; reset; run "input tap $ITEM"; sleep 1.5; save push1
      reset; run "input keyevent 4"; sleep 1.5; save pop1;;
    pushpop_gesture)
      run "input tap $HOME_X $TAB_Y"; sleep 1.5; reset
      run "for i in 1 2 3 4 5 6; do input tap $ITEM; sleep 1.3; input swipe 3 1200 760 1250 260; sleep 1.3; done"
      sleep 1; save pushpop_gesture;;
    scroll_home)
      run "input tap $HOME_X $TAB_Y"; sleep 1.5; reset
      run "for i in 1 2 3 4 5 6; do input swipe 540 1800 540 700 140; sleep 0.7; done; for i in 1 2 3 4 5 6; do input swipe 540 700 540 1800 140; sleep 0.7; done"
      sleep 1; save scroll_home;;
    scroll_feed)
      run "input tap $FEED_X $TAB_Y"; sleep 2.5; reset
      run "for i in 1 2 3 4 5 6; do input swipe 540 1800 540 700 140; sleep 0.7; done; for i in 1 2 3 4 5 6; do input swipe 540 700 540 1800 140; sleep 0.7; done"
      sleep 1; save scroll_feed; run "input tap $HOME_X $TAB_Y"; sleep 1;;
  esac
done
