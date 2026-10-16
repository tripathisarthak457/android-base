# tile.sh <framedir> <first> <step> <count> <out.png> [cols] [w]
d=$1; first=$2; step=$3; count=$4; out=$5; cols=${6:-6}; w=${7:-216}
rows=$(( (count + cols - 1) / cols ))
tmp=$(mktemp -d)
i=0
mapfile -t T < $d/times.txt
for ((k=0;k<count;k++)); do n=$((first + k*step)); f=$(printf "%s/f_%04d.png" $d $n); [ -f $f ] || break
  t=${T[$((n-1))]}; ms=$(python3 -c "print(int(float('$t')*1000))")
  ffmpeg -v error -y -i $f -vf "scale=$w:-1,drawtext=text='#$n  ${ms}ms':x=4:y=4:fontsize=16:fontcolor=red:box=1:boxcolor=white@0.8" $tmp/$(printf "%03d" $i).png 2>/dev/null || ffmpeg -v error -y -i $f -vf "scale=$w:-1" $tmp/$(printf "%03d" $i).png
  i=$((i+1)); done
ffmpeg -v error -y -framerate 1 -i $tmp/%03d.png -vf "tile=${cols}x${rows}:padding=4:color=black" -frames:v 1 $out 2>/dev/null
rm -rf $tmp; ls -la $out
